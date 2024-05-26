package model.hotel;

import com.mongodb.client.MongoDatabase;
import controllers.UserType;
import model.Database;
import model.Guest;
import model.User;
import model.supervisors.Worker;

import java.util.HashMap;

public class Hotel {
    public static final MongoDatabase hotelDatabase = Database.getDatabase();
    private static final String name = "L'Oasis";
    private static final String location = "L'Oasis Hotel Rue du Paradis 56 Beskra";
    protected static final HashMap<String, Guest> guests= new HashMap<>();
    protected static final HashMap<String, Reservation> reservationRequests= new HashMap<>();

    protected static final HashMap<String, Room> rooms= new HashMap<>();
    protected static final HashMap<String, Worker> workers = new HashMap<>();
    protected static User user;

    /**
     * @param userType UserType
     * */
    public static void initHotelModel(UserType userType) {
        //Retrieving rooms from the database and putting them in the rooms HashMap
        try {
            if (rooms.isEmpty())
                Database.retrieveRoomsFromDB(rooms);
        } catch (Database.DBException e) {
            System.out.println(e.getMessage());
        }

        switch (userType){
            case GUEST -> {
                try{
                    System.out.print("im inside Guest");
                    Database.retrieveReservationsFromDB(reservationRequests);

                }catch (Database.DBException e) {
                    System.out.println(e.getMessage());
                }
            }
            case RECEPTIONIST -> {
               try {

                   Database.retrieveGuestsFromDB(guests);
                   Database.retrieveReservationsFromDB(reservationRequests);
                   System.out.println(reservationRequests);
                    for (Guest guest : guests.values()) {
                        if (guest.getReservations().isEmpty()) {
                            continue;
                        }
                    }
                   for (Reservation reservation : reservationRequests.values()) {
                       if (!reservation.isPaid())
                           reservationRequests.put(reservation.getRoomNumber(), reservation);
                   }
               }catch (Database.DBException e){
                    System.out.println(e.getMessage());
               }
            }
            case MANAGER -> {
                try{
                    Database.retrieveGuestsFromDB(guests);
                    Database.retrieveWorkersFromDB(workers);
                    Database.retrieveReservationsFromDB(reservationRequests);

                    for (Guest guest : guests.values()) {
                        if (guest.getReservations().isEmpty()) {
                            continue;
                        }
                        for (Reservation reservation : guest.getReservations().values()) {
                            if (!reservation.isPaid())
                                reservationRequests.put(reservation.getRoomNumber(), reservation);
                        }
                    }
                }catch (Database.DBException e){
                    System.out.println(e.getMessage());
                }
            }
        }
    }
    public static String getName() {
        return name;
    }
    public static String getLocation() {
        return location;
    }
    public static HashMap<String,Guest> getGuests() {
        return guests;
    }
    public static HashMap<String,Worker> getWorkers() {
        return workers;
    }
    public static HashMap<String, Room> getRooms() {
        return rooms;
    }
    public static HashMap<String, Reservation> getReservationRequests() {
        return reservationRequests;
    }

    public static void setUser(User user) {
        Hotel.user = user;
    }
    public static User getUser() {
        return user;
    }
}