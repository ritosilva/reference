package pt.ulisboa.tecnico.softeng.broker.domain

import spock.lang.Unroll

import pt.ulisboa.tecnico.softeng.broker.services.remote.ActivityInterface
import pt.ulisboa.tecnico.softeng.broker.services.remote.BankInterface
import pt.ulisboa.tecnico.softeng.broker.services.remote.CarInterface
import pt.ulisboa.tecnico.softeng.broker.services.remote.HotelInterface
import pt.ulisboa.tecnico.softeng.broker.services.remote.TaxInterface
import pt.ulisboa.tecnico.softeng.broker.services.remote.exception.RemoteAccessException
import pt.ulisboa.tecnico.softeng.broker.services.remote.exception.BankException

class ProcessPaymentStateProcessMethodSpockTest extends SpockRollbackTestAbstractClass  {

    def broker
    def bankInterface
    def client
    def adventure

    @Override
    def populate4Test() {
        bankInterface = Mock(BankInterface)
        broker = new Broker('BR01', 'eXtremeADVENTURE', BROKER_NIF_AS_SELLER, NIF_AS_BUYER, BROKER_IBAN,
                new ActivityInterface(), new HotelInterface(), new CarInterface(), bankInterface, new TaxInterface())
        client = new Client(broker, CLIENT_IBAN, CLIENT_NIF, DRIVING_LICENSE, AGE)
        adventure = new Adventure(broker, BEGIN, END, client, MARGIN)

        adventure.setState(Adventure.State.PROCESS_PAYMENT)
    }

    def 'success payment'() {
        given: 'the payment is successful'
        bankInterface.processPayment(_) >> PAYMENT_CONFIRMATION

        when: 'a next step in the adventure is processed'
        adventure.process()

        then: 'the adventure state progresses to tax payment'
        adventure.getState().getValue() == Adventure.State.TAX_PAYMENT
        and: 'the tax is confirmed'
        adventure.getPaymentConfirmation() == PAYMENT_CONFIRMATION
    }

    @Unroll('#process_iterations #exception is thrown')
    def '#process_iterations #exception exception'() {
        given: 'the payment throws an exception'
        bankInterface.processPayment(_) >> { throw mock_exception }

        when: 'a next step in the adventure is processed'
        1.upto(process_iterations) { adventure.process() }

        then: 'the adventure state progresses to undo'
        adventure.getState().getValue() == state
        and: 'the tax confirmation is null'
        adventure.getPaymentConfirmation() == null

        where:
        mock_exception              | state                           | process_iterations                        | exception
        new BankException()         | Adventure.State.CANCELLED       | 2                                         | 'BankException'
        new RemoteAccessException() | Adventure.State.PROCESS_PAYMENT | 1                                         | 'RemoteAccessException'
        new RemoteAccessException() | Adventure.State.CANCELLED       | 4                                         | 'RemoteAccessException'
        new RemoteAccessException() | Adventure.State.PROCESS_PAYMENT | ProcessPaymentState.MAX_REMOTE_ERRORS - 1 | 'RemoteAccessException'
        new RemoteAccessException() | Adventure.State.UNDO            | ProcessPaymentState.MAX_REMOTE_ERRORS     | 'RemoteAccessException'
    }

    def 'two remote access exception one success'() {
        given: 'the payment throws a remote access exception and they return a reference'
        bankInterface.processPayment(_) >>
                { throw new RemoteAccessException() } >>
                { throw new RemoteAccessException() } >>
                PAYMENT_CONFIRMATION

        when: 'the adventure is processed 4 times'
        1.upto(4) { adventure.process() }

        then: 'the adventure state progresses to confirmed'
        adventure.getState().getValue() == Adventure.State.TAX_PAYMENT
        and: 'the payment is confirmed'
        adventure.getPaymentConfirmation() == PAYMENT_CONFIRMATION
    }

    def 'one remote access exception and one bank exception'() {
        given: 'the tax payment throws a remote access exception'
        bankInterface.processPayment(_) >> { throw new RemoteAccessException() }

        when: 'the adventure is processed'
        adventure.process()

        then: 'the adventure state is not changed'
        adventure.getState().getValue() == Adventure.State.PROCESS_PAYMENT
        and: 'the number of errors is 1'
        adventure.getState().getNumOfRemoteErrors() == 1

        when: 'the adventure is processed again'
        adventure.process()

        then: 'the tax payment throws a tax exception'
        bankInterface.processPayment(_) >> { throw new BankException() }
        and: 'the adventure state progresses to undo'
        adventure.getState().getValue() == Adventure.State.UNDO
        and: 'the tax payment confirmation is null'
        adventure.getPaymentConfirmation() == null
    }
}
