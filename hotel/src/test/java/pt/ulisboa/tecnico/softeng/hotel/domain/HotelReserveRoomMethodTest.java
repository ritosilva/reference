package pt.ulisboa.tecnico.softeng.hotel.domain;

import static junit.framework.TestCase.assertTrue;

import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import pt.ulisboa.tecnico.softeng.hotel.exception.HotelException;
import pt.ulisboa.tecnico.softeng.hotel.interfaces.BankInterface;
import pt.ulisboa.tecnico.softeng.hotel.interfaces.TaxInterface;
import pt.ulisboa.tecnico.softeng.tax.dataobjects.InvoiceData;

@RunWith(JMockit.class)
public class HotelReserveRoomMethodTest {
	private final LocalDate arrival = new LocalDate(2016, 12, 19);
	private final LocalDate departure = new LocalDate(2016, 12, 24);
	private Room room;
	private Hotel hotel;
	private static final String NIF_HOTEL = "123456789";
	private static final String NIF_BUYER = "123456700";
	private static final String IBAN_BUYER = "IBAN_CUSTOMER";
	private static final String IBAN_HOTEL = "IBAN_HOTEL";

	@Mocked
	private TaxInterface taxInterface;
	@Mocked
	private BankInterface bankInterface;

	@Before
	public void setUp() {
		this.hotel = new Hotel("XPTO123", "Lisboa", NIF_HOTEL, IBAN_HOTEL, 20.0, 30.0);
		this.room = new Room(this.hotel, "01", Room.Type.SINGLE);
	}

	@Test
	public void success() {
		new Expectations() {
			{
				BankInterface.processPayment(this.anyString, this.anyDouble);

				TaxInterface.submitInvoice((InvoiceData) this.any);
			}
		};

		final String ref = Hotel.reserveRoom(Room.Type.SINGLE, this.arrival, this.departure, NIF_BUYER, IBAN_BUYER);

		assertTrue(ref != null);
		assertTrue(ref.startsWith("XPTO123"));
	}

	@Test(expected = HotelException.class)
	public void noHotels() {
		Hotel.hotels.clear();
		Hotel.reserveRoom(Room.Type.SINGLE, this.arrival, this.departure, NIF_BUYER, IBAN_BUYER);
	}

	@Test(expected = HotelException.class)
	public void noVacancy() {
		Hotel.reserveRoom(Room.Type.SINGLE, this.arrival, new LocalDate(2016, 12, 25), NIF_BUYER, IBAN_BUYER);
		Hotel.reserveRoom(Room.Type.SINGLE, this.arrival, new LocalDate(2016, 12, 25), NIF_BUYER, IBAN_BUYER);
	}

	@Test(expected = HotelException.class)
	public void noRooms() {
		this.hotel.removeRooms();
		Hotel.reserveRoom(Room.Type.SINGLE, this.arrival, new LocalDate(2016, 12, 25), NIF_BUYER, IBAN_BUYER);
	}

	@After
	public void tearDown() {
		Hotel.hotels.clear();
	}

}