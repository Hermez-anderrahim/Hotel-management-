package controllers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import model.Database;
import model.Guest;
import model.User;
import model.hotel.Hotel;
import model.hotel.Reservation;
import model.hotel.Room;
import model.hotel.RoomType;
import model.supervisors.*;
import net.miginfocom.swing.MigLayout;
import org.bson.Document;
import org.jdesktop.swingx.JXDatePicker;
import view.UserGui.GuestUi;
import view.UserGui.ManagerGui;
import view.UserGui.ReceptionistGui;
import view.components.Message;
import view.components.OurButton;
import view.components.items.MyTextField;
import view.components.roomComponents.CounterPanel;
import view.components.roomComponents.RoomOnList;
import view.components.roomComponents.RoomUI;
import view.components.table.Table;
import view.login.container.ForgetPassword;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Its purpose is to keep the UI in sync with the data, and fire the appropriate
 * events when an interaction happens.
 */
public class Controller {
    private static ManagerGui managerGui;
    private static GuestUi guestUi = null;
    private final static String MANAGER_EMAIL = "admin";
    private final static String MANAGER_PASSWORD = "admin12345#";
    public final static String DOMAIN_RECEPTIONIST = "@Oasis.dz";

    /**
     * This method is used to set the user data in the Model and open the
     * appropriate UI
     * 
     * @param user The user to be set
     */
    public static void setHotelUserAndOpenUI(User user) {
        Hotel.setUser(user);
        if (user instanceof Guest) {
            Hotel.initHotelModel(UserType.GUEST);
            guestUi = new GuestUi((Guest) user);
            GuestUi.run(guestUi);
        }
        if (user instanceof Worker) {
            if (((Worker) user).getRole().equals(Role.RECEPTIONIST)) {
                Hotel.initHotelModel(UserType.RECEPTIONIST);
                ReceptionistGui.run(new ReceptionistGui((Receptionist) user));
            }
        }
        if (user == null) {
            Hotel.initHotelModel(UserType.MANAGER);
            managerGui = new ManagerGui(null, Hotel.getRooms().size(), Hotel.getWorkers().size(),
                    Hotel.getGuests().size());
            ManagerGui.run(managerGui);
        }
    }

    public static void checkRegistration(JButton btnRegister, JTextField nameInput, JTextField lastNameInput,
            JTextField emailInput, JPasswordField passwordInput, Message msg, JPanel bg, MigLayout layout) {

        btnRegister.addActionListener(e -> {
            String name = nameInput.getText().trim();
            String lastName = lastNameInput.getText().trim();
            String email = emailInput.getText().trim();
            char[] passwordChars = passwordInput.getPassword();

            if (name.isEmpty() || lastName.isEmpty() || email.isEmpty() || passwordChars.length == 0) {
                msg.displayMessage(Message.MessageType.ERROR, "All fields are required", bg, layout);
                return;
            }

            if (!isValidEmail(email)) {
                msg.displayMessage(Message.MessageType.ERROR, "Invalid email format", bg, layout);
                return;
            }

            if (passwordChars.length < 8) {
                msg.displayMessage(Message.MessageType.ERROR, "Password must be at least 8 characters long", bg,
                        layout);
                return;
            }

            try {
                checkPasswordStrength(passwordChars);
                String password = new String(passwordChars).trim();
                addGuestFromInputs(name, lastName, email, password);
                msg.displayMessage(Message.MessageType.SUCCESS, "Registered successfully", bg, layout);
            } catch (AccessAppException exception) {
                msg.displayMessage(Message.MessageType.ERROR, exception.getMessage(), bg, layout);
            }
        });
    }

    private static void checkPasswordStrength(char[] passwordChars) throws AccessAppException {
        boolean containsCapitalLetter = false;
        boolean containsSpecialCharacter = false;

        for (char c : passwordChars) {
            if (Character.isUpperCase(c)) {
                containsCapitalLetter = true;
            } else if (!Character.isLetterOrDigit(c)) {
                containsSpecialCharacter = true;
            }

            if (containsCapitalLetter && containsSpecialCharacter) {
                return; // Password meets the criteria, exit the loop
            }
        }

        // If the loop completes without meeting both criteria
        throw new AccessAppException("Password must contain at least one capital letter and one special character");
    }

    public static void checkLogin(JButton btn, JTextField emailInput, JPasswordField passwordInput, Message msg,
            JPanel bg, MigLayout layout) throws RuntimeException {
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                User loginUser;
                String email = emailInput.getText();
                String password = String.valueOf(passwordInput.getPassword());
                try {
                    if (email.isEmpty() || password.isEmpty()) {
                        throw new AccessAppException("All the Fields Are Required");
                    }
                    if (email.equals(MANAGER_EMAIL) && password.equals(MANAGER_PASSWORD)) {
                        setHotelUserAndOpenUI(null);
                        SwingUtilities.getWindowAncestor(bg).dispose(); // Close the login form
                        return;
                    }
                    if (!email.endsWith(DOMAIN_RECEPTIONIST) && !isValidEmail(email)) {
                        throw new AccessAppException("Invalid Email");
                    }
                    if (email.endsWith(DOMAIN_RECEPTIONIST)) { // Receptionist
                        loginUser = getUserFromModel("Workers", "OasisMail", email);
                    } else {
                        loginUser = getUserFromModel("Guests", "email", email);
                        if (loginUser == null)
                            loginUser = getUserFromModel("Workers", "email", email);
                    }

                    if (loginUser == null) {
                        throw new AccessAppException("User not found");
                    } else {
                        String truePassword = loginUser.getPassword();
                        if (!PasswordHashing.verifyPassword(password, truePassword)) {
                            throw new AccessAppException("Incorrect Password");
                        } else {
                            setHotelUserAndOpenUI(loginUser);
                        }
                        SwingUtilities.getWindowAncestor(bg).dispose(); // Close the login form
                    }
                } catch (AccessAppException ex) {
                    msg.displayMessage(Message.MessageType.ERROR, ex.getMessage(), bg, layout); // Display error message
                }
            }
        });

    }

    public static void launchForgotPasswordUI(JButton btnRegisterForget, Component c) {
        btnRegisterForget.addActionListener(e -> {
            ForgetPassword forgetPasswordPanel = new ForgetPassword();
            JFrame parentContainer = (JFrame) SwingUtilities.getWindowAncestor(c);
            parentContainer.setContentPane(forgetPasswordPanel);
            parentContainer.revalidate();
            parentContainer.repaint();
        });
    }

    private static void addGuestFromInputs(String firstName, String lastName, String email, String password) {
        Guest guest = new Guest(firstName, lastName, email, PasswordHashing.hashPassword(password));
        Receptionist.addGuestToDataBase(guest);
        Hotel.getGuests().put(email, guest);
    }

    /**
     * Allows searching for a user by various keys (FOR Guest OR Worker)
     * 
     * @param collectionName The name of the collection
     * @param researchBy     The field to search by
     * @param matchingField  The value to match
     * @return User user or null
     */
    public static User getUserFromModel(String collectionName, String researchBy, String matchingField) {
        switch (collectionName) {
            case "Guests" -> {
                if (researchBy.equals("email"))
                    if (!Hotel.getGuests().isEmpty() && Hotel.getGuests().containsKey(matchingField)) {
                        return Hotel.getGuests().get(matchingField);
                    } else if (Hotel.getGuests().isEmpty()) {
                        Document guestDoc = Database.findInDataBase(collectionName, researchBy, matchingField);
                        if (guestDoc != null) {
                            Document reservationsDocument = (Document) guestDoc.get("Reservations");
                            // Convert the Document back to a HashMap
                            HashMap<String, Object> reservationsHashMap = new HashMap<>(reservationsDocument);
                            // Convert each Object in reservationsHashMap to a Reservation
                            HashMap<String, Reservation> reservations = new HashMap<>();
                            for (Map.Entry<String, Object> entry : reservationsHashMap.entrySet()) {
                                Document reservationDocument = (Document) entry.getValue();
                                reservations.put(entry.getKey().replaceAll("-", "."),
                                        Reservation.fromDocument(reservationDocument));
                            }
                            Guest guest = new Guest(guestDoc.getString("firstName"),
                                    guestDoc.getString("lastName"),
                                    guestDoc.getString("email"),
                                    guestDoc.getString("password"));
                            guest.setReservations(reservations);
                            return guest;
                        }
                    }
            }
            case "Workers" -> {
                if (researchBy.equals("OasisMail"))
                    if (!Hotel.getWorkers().isEmpty() && Hotel.getWorkers().containsKey(matchingField)) {
                        return Hotel.getWorkers().get(matchingField);
                    } else if (Hotel.getWorkers().isEmpty()) {
                        Document workerDoc = Database.findInDataBase(collectionName, researchBy, matchingField);
                        if (workerDoc != null) {
                            Worker worker;
                            if (workerDoc.getString("role").equals("RECEPTIONIST")) {
                                worker = new Receptionist(workerDoc.getString("firstName"),
                                        workerDoc.getString("lastName"),
                                        workerDoc.getString("email"));
                                worker.setOasisMail(workerDoc.getString("OasisMail"));
                                worker.setPassword(workerDoc.getString("password"));
                            } else {
                                worker = new Others(workerDoc.getString("firstName"),
                                        workerDoc.getString("lastName"),
                                        workerDoc.getString("email"));
                                worker.setOasisMail(workerDoc.getString("OasisMail"));
                                worker.setPassword(workerDoc.getString("password"));
                            }
                            return worker;
                        }
                    }
                if (researchBy.equals("email")) {
                    if (!Hotel.getWorkers().isEmpty()) {
                        for (Worker receptionist : Hotel.getWorkers().values()) {
                            if (receptionist.getEmail().equals(matchingField)) {
                                return receptionist;
                            }
                        }
                    } else {
                        Document workerDoc = Database.findInDataBase(collectionName, researchBy, matchingField);
                        if (workerDoc != null) {
                            Worker worker;
                            if (workerDoc.getString("role").equals("RECEPTIONIST")) {
                                worker = new Receptionist(workerDoc.getString("firstName"),
                                        workerDoc.getString("lastName"),
                                        workerDoc.getString("email"));
                                worker.setOasisMail(workerDoc.getString("OasisMail"));
                                worker.setPassword(workerDoc.getString("password"));
                            } else {
                                worker = new Others(workerDoc.getString("firstName"),
                                        workerDoc.getString("lastName"),
                                        workerDoc.getString("email"));
                                worker.setOasisMail(workerDoc.getString("OasisMail"));
                                worker.setPassword(workerDoc.getString("password"));
                            }
                            return worker;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get all documents from the database without the password field, and return
     * them to views as a 2D array
     * 
     * @param collectionName The name of the collection
     * @param columnNames    The names of documents to retrieve
     */
    public static void initiateTable(String collectionName, String[] columnNames, Table table) {
        try {
            MongoCollection<Document> collection = Hotel.hotelDatabase.getCollection(collectionName);
            try (MongoCursor<Document> cursor = collection.find().iterator()) {
                while (cursor.hasNext()) {
                    Document document = cursor.next();

                    if (document.containsKey("password"))
                        document.remove("password");

                    Object[] row = new String[columnNames.length];
                    for (int i = 0; i < columnNames.length; i++) {
                        row[i] = document.getString(columnNames[i]);
                    }
                    table.addRow(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initTableResReq(String[] reqColumnNames, Table reqTable, String[] resColumnNames,
            Table resTable) {
        if (getUser() instanceof Guest) {
            try {
                Guest user = (Guest) getUser();
                for (Reservation r : user.getReservations().values()) {
                    if (!r.isReservation()) {
                        Object[] row = new Object[reqColumnNames.length];
                        row[0] = r.getRoomNumber();
                        row[1] = user.getEmail();
                        row[2] = r.getPhoneNumber();
                        row[3] = r.getCheckInDate();
                        row[4] = r.getCheckOutDate();
                        row[5] = r.getAdults();
                        row[6] = r.getChildren();
                        row[7] = r.getTotalCost();
                        row[8] = r.isPaid();
                        row[9] = r.isConfirmed();
                        reqTable.addRow(row);
                    } else {
                        Object[] row = new Object[resColumnNames.length];
                        row[0] = r.getRoomNumber();
                        row[1] = user.getEmail();
                        row[2] = r.getPhoneNumber();
                        row[3] = r.getCheckInDate();
                        row[4] = r.getCheckOutDate();
                        row[5] = r.getAdults();
                        row[6] = r.getChildren();
                        row[7] = r.getTotalCost();
                        row[8] = r.isPaid();
                        resTable.addRow(row);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                for (Reservation r : Hotel.getReservationRequests().values()) {
                    if (!r.isReservation()) {
                        Object[] row = new Object[reqColumnNames.length];
                        row[0] = r.getRoomNumber();
                        row[1] = r.getGuestEmail();
                        row[2] = r.getPhoneNumber();
                        row[3] = r.getCheckInDate();
                        row[4] = r.getCheckOutDate();
                        row[5] = r.getAdults();
                        row[6] = r.getChildren();
                        row[7] = r.getTotalCost();
                        row[8] = r.isPaid();
                        row[9] = r.isConfirmed();
                        reqTable.addRow(row);
                    } else {
                        Object[] row = new Object[resColumnNames.length];
                        row[0] = r.getRoomNumber();
                        row[1] = r.getGuestEmail();
                        row[2] = r.getPhoneNumber();
                        row[3] = r.getCheckInDate();
                        row[4] = r.getCheckOutDate();
                        row[5] = r.getAdults();
                        row[6] = r.getChildren();
                        row[7] = r.getTotalCost();
                        row[8] = r.isPaid();
                        resTable.addRow(row);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void addGuest(String firstName, String lastName, String email, Table table)
            throws AccessAppException {
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            throw new AccessAppException("All the fields are required");
        } else if (!isValidEmail(email)) {
            throw new AccessAppException("Invalid Email");
        }
        Guest guest = (Guest) getUserFromModel("Guests", "email", email);
        if (guest != null) {
            throw new AccessAppException("Email already exists");
        }
        addGuestFromInputs(firstName, lastName, email, firstName + lastName + "123");
        table.addRow(new Object[] { firstName, lastName, email });
        if (managerGui != null && !(Hotel.getUser() instanceof Receptionist))
            managerGui.getWelcomePage().updateCard("Guests", Hotel.getGuests().size());
    }

    public static void removeGuest(String email, Table table) throws AccessAppException {
        int row = table.getSelectedRow();
        if (row == -1) {
            return;
        }
        if (email.isEmpty()) {
            throw new AccessAppException("Email field is required");
        } else if (!isValidEmail(email)) {
            throw new AccessAppException("Invalid Email");
        }
        if (!Hotel.getGuests().containsKey(email)) {
            throw new AccessAppException("Email does not exist!");
        }
        table.deleteRow(row);
        Receptionist.removeGuestFromDataBase(email);
        if (managerGui != null && !(Hotel.getUser() instanceof Receptionist))
            managerGui.getWelcomePage().updateCard("Guests", Hotel.getGuests().size());
    }

    public static void clearGuests(Table table) {
        int ans = JOptionPane.showOptionDialog(null,
                "Are you sure you want to delete all guests?", "WARNING",
                JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, null, null);
        Manager.clearData("Guests", ans == 0);
        if (ans == 0)
            table.clearTable();
        if (managerGui != null && !(Hotel.getUser() instanceof Receptionist))
            managerGui.getWelcomePage().updateCard("Guests", Hotel.getGuests().size());
    }

    public static void addReceptionist(String firstName, String lastName, String email, String oasisMail, Table table)
            throws AccessAppException {
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || oasisMail.isEmpty()) {
            throw new AccessAppException("All the Fields Are Required");
        } else if (!isValidEmail(email)) {
            throw new AccessAppException("Invalid Email");
        }
        // Remove all digits from the oasisMail to use it in next process and avoid
        // mutation
        StringBuilder sb = new StringBuilder(oasisMail);
        for (int i = 0; i < sb.length(); i++) {
            if (Character.isDigit(sb.charAt(i))) {
                sb.deleteCharAt(i);
                i--;
            }
        }
        // Check if the email is already used by other user
        Worker receptionist = (Worker) getUserFromModel("Workers", "OasisMail", sb.toString());
        if (receptionist != null && (receptionist.getEmail().equals(email))) {
            throw new AccessAppException("Email already exists!");
        }
        if (getUserFromModel("Guests", "email", email) != null) {
            throw new AccessAppException("Email already exists!");
        }
        receptionist = new Receptionist(firstName, lastName, email);
        receptionist.setOasisMail(oasisMail);
        receptionist.setPassword(PasswordHashing.hashPassword(firstName + lastName + "123"));
        Manager.addWorkerToDataBase(receptionist);
        Hotel.getWorkers().put(oasisMail, receptionist);
        table.addRow(new Object[] { firstName, lastName, email, oasisMail });
        if (managerGui != null && !(Hotel.getUser() instanceof Receptionist))
            managerGui.getWelcomePage().updateCard("Workers", Hotel.getWorkers().size());
    }

    public static void removeReceptionist(String oasisMail, Table table) throws AccessAppException {
        int row = table.getSelectedRow();
        if (row == -1) {
            return;
        }
        if (oasisMail.isEmpty()) {
            throw new AccessAppException("Email field is required");
        }
        if (!Hotel.getWorkers().containsKey(oasisMail)) {
            throw new AccessAppException("Email does not exist!");
        }
        table.deleteRow(row);
        Manager.removeWorkerFromDataBase(oasisMail);
        if (managerGui != null && !(Hotel.getUser() instanceof Receptionist))
            managerGui.getWelcomePage().updateCard("Workers", Hotel.getWorkers().size());
    }

    public static void clearReceptionists(Table table) {
        int ans = JOptionPane.showOptionDialog(null,
                "Are you sure you want to delete all receptionists?", "WARNING",
                JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, null, null);
        Manager.clearData("Workers", ans == 0);
        if (ans == 0)
            table.clearTable();
        if (managerGui != null && !(Hotel.getUser() instanceof Receptionist))
            managerGui.getWelcomePage().updateCard("Workers", Hotel.getWorkers().size());
    }

    public static void addReservationReceptionistVersion(OurButton btn, Table reqTable, Table resTable, Message msg,
            JPanel bg, MigLayout layout) {
        btn.addActionListener(e -> {
            int row = reqTable.getSelectedRow();
            if (row == -1) {
                return;
            }
            boolean isConfirmed = (boolean) reqTable.getValueAt(row, 9);
            if (!isConfirmed) {
                msg.displayMessage(Message.MessageType.ERROR, "Reservation is not confirmed", bg, layout);
                return;
            }
            String roomNumber = (String) reqTable.getValueAt(row, 0);
            String email = (String) reqTable.getValueAt(row, 1);
            String phoneNumber = (String) reqTable.getValueAt(row, 2);
            OurDate checkInDate = (OurDate) reqTable.getValueAt(row, 3);
            OurDate checkOutDate = (OurDate) reqTable.getValueAt(row, 4);
            int adults = (int) reqTable.getValueAt(row, 5);
            int children = (int) reqTable.getValueAt(row, 6);
            double totalCost = (double) reqTable.getValueAt(row, 7);
            boolean isPaid = (boolean) reqTable.getValueAt(row, 8);
            reqTable.deleteRow(row);
            try {
                Reservation res = Hotel.getGuests().get(email).getReservations().get(roomNumber + email
                        + checkInDate.getDay() + "/" + checkInDate.getMonth() + "/" + checkInDate.getYear());
                res.setReservation(true);
                handleUpdates("Guest", roomNumber, email, "Reservations", res, null);
            } catch (ClassCastException ex) {
                ex.printStackTrace();
            } catch (AccessAppException ex) {
                System.out.println(ex.getMessage());
            }
            resTable.addRow(new Object[] { roomNumber, email, phoneNumber, checkInDate, checkOutDate, adults, children,
                    totalCost, isPaid });
        });
    }

    public static void payReservation(OurButton btn, Table resTable, Message msg, JPanel bg, MigLayout layout) {
        btn.addActionListener(e -> {
            int row = resTable.getSelectedRow();
            if (row == -1) {
                return;
            }
            String roomNumber = (String) resTable.getValueAt(row, 0);
            String email = (String) resTable.getValueAt(row, 1);
            OurDate checkInDate = (OurDate) resTable.getValueAt(row, 3);
            resTable.setValueAt(true, row, 8);
            try {
                Reservation res = Hotel.getGuests().get(email).getReservations().get(roomNumber + email
                        + checkInDate.getDay() + "/" + checkInDate.getMonth() + "/" + checkInDate.getYear());
                res.setPaid(true);
                handleUpdates("Guest", roomNumber, email, "Reservations", res, null);
                msg.displayMessage(Message.MessageType.SUCCESS, "Reservation paid !", bg, layout);
            } catch (ClassCastException ex) {
                ex.printStackTrace();
            } catch (AccessAppException ex) {
                System.out.println("Error in payReservation: " + ex.getMessage());
            }
        });
    }

    public static void confirmReservation(OurButton btn, Table reqTable, Message msg, JPanel bg, MigLayout layout,
            LinkedList<String> confirmedReservations) {
        btn.addActionListener(e -> {
            int row = reqTable.getSelectedRow();
            if (row == -1) {
                return;
            }
            String roomNumber = (String) reqTable.getValueAt(row, 0);
            String email = (String) reqTable.getValueAt(row, 1);
            OurDate checkInDate = (OurDate) reqTable.getValueAt(row, 3);
            try {
                Guest user = (Guest) getUser();
                Reservation res = user.getReservations().get(roomNumber + email + checkInDate.toString());
                res.setConfirmed(true);
                handleUpdates("Guest", roomNumber, email, "Reservations", res, null);
                reqTable.setValueAt(true, row, 9);
                msg.displayMessage(Message.MessageType.SUCCESS, "Reservation confirmed successfully, wait for response",
                        bg, layout);
                confirmedReservations.add(roomNumber + email + checkInDate.toString());
            } catch (ClassCastException ex) {
                msg.displayMessage(Message.MessageType.ERROR, "An error occurred while confirming request!", bg,
                        layout);
            } catch (AccessAppException ex) {
                msg.displayMessage(Message.MessageType.ERROR, "Error accessing th app", bg, layout);
            }
        });
    }

    public static void cancelReservation(OurButton btn, Table reqTable, Message msg, JPanel bg, MigLayout layout) {
        btn.addActionListener(e -> {
            try {
                int row = reqTable.getSelectedRow();
                if (row == -1) {
                    return;
                }
                String roomNumber = (String) reqTable.getValueAt(row, 0);
                OurDate checkInDate = (OurDate) reqTable.getValueAt(row, 3);
                reqTable.deleteRow(row);

                Guest user = (Guest) getUser();
                user.getReservations().remove(roomNumber + user.getEmail() + checkInDate.toString());
                Database.removeReservationFromGuest(user.getEmail(), roomNumber + user.getEmail() + checkInDate.getDay()
                        + "/" + checkInDate.getMonth() + "/" + checkInDate.getYear());

                Collection<RoomOnList> roomsOnList = guestUi.getRoomsPanel().getRooms().values();
                for (RoomOnList roomOnList : roomsOnList) {
                    if (roomOnList.getUsedRoomNumbers().contains(roomNumber)) {
                        roomOnList.getUsedRoomNumbers().remove(roomNumber);
                        roomOnList.getRoomNumbers().add(roomNumber);

                        // TODO make room details shows "Available"
                        roomOnList.setAvailable(true);
                        roomOnList.repaint();
                        roomOnList.revalidate();
                        roomOnList.getRoomDetail().setAvailable(true);
                        guestUi.revalidate();
                        break;
                    }
                }
                handleUpdates("Room", roomNumber, null, "isAvailable", true, null);
                msg.displayMessage(Message.MessageType.SUCCESS, "Reservation canceled successfully", bg, layout);
            } catch (ClassCastException | AccessAppException ex) {
                msg.displayMessage(Message.MessageType.ERROR, "An error occurred while cancelling request!", bg,
                        layout);
            }
        });
    }

    public static void updateReservation(OurButton btn, Table reqTable, JXDatePicker checkIn, JXDatePicker checkOut,
            CounterPanel adults, CounterPanel children, MyTextField phoneNumber,
            MyTextField totalCost, Message msg, JPanel bg, MigLayout layout, OurButton deleteBtn, OurButton confirmBtn,
            LinkedList<String> confirmedRequests) {
        btn.addActionListener(e -> {
            int row = reqTable.getSelectedRow();
            if (row == -1) {
                return;
            }

            String roomNumber = (String) reqTable.getValueAt(row, 0);
            String email = (String) reqTable.getValueAt(row, 1);

            String[] tmpDate1 = reqTable.getValueAt(row, 3).toString().split("/");
            OurDate oldCheckInDate = new OurDate(Integer.parseInt(tmpDate1[0]), Integer.parseInt(tmpDate1[1]),
                    Integer.parseInt(tmpDate1[2]));

            String[] tmpDate2 = reqTable.getValueAt(row, 4).toString().split("/");
            OurDate oldCheckOutDate = new OurDate(Integer.parseInt(tmpDate2[0]), Integer.parseInt(tmpDate2[1]),
                    Integer.parseInt(tmpDate2[2]));

            int oldAdults = (int) reqTable.getValueAt(row, 5);
            int oldChildren = (int) reqTable.getValueAt(row, 6);

            if (confirmedRequests.contains(roomNumber + email + oldCheckInDate.toString())) {
                msg.displayMessage(Message.MessageType.ERROR, "Reservation is confirmed, you can't edit it", bg,
                        layout);
                return;
            }

            if (btn.getText().equals("Edit")) {
                btn.setText("Confirm edit");
                confirmBtn.setEnabled(false);
                confirmBtn.setButtonBgColor(Color.GRAY);
                deleteBtn.setEnabled(false);
                deleteBtn.setButtonBgColor(Color.GRAY);
                checkIn.setEnabled(true);
                checkOut.setEnabled(true);
                adults.getIncrementButton().setEnabled(true);
                children.getIncrementButton().setEnabled(true);
                adults.getDecrementButton().setEnabled(true);
                children.getDecrementButton().setEnabled(true);
                phoneNumber.setEnabled(true);
                return;
            } else {
                btn.setText("Edit");
                confirmBtn.setEnabled(true);
                confirmBtn.setButtonBgColor(new Color(0, 112, 255));
                deleteBtn.setEnabled(true);
                deleteBtn.setButtonBgColor(new Color(0xED1B24));
                checkIn.setEnabled(false);
                checkOut.setEnabled(false);
                adults.getIncrementButton().setEnabled(false);
                children.getIncrementButton().setEnabled(false);
                adults.getDecrementButton().setEnabled(false);
                children.getDecrementButton().setEnabled(false);
                phoneNumber.setEnabled(false);
            }
            try {
                if (checkIn.getDate() == null || checkOut.getDate() == null || phoneNumber.getText().isEmpty()
                        || !phoneNumber.getText().matches("[0-9]+") || phoneNumber.getText().length() != 10) {
                    throw new AccessAppException("All the fields are required");
                }
                Date checkInDate = checkIn.getDate();
                Calendar checkInCalender = Calendar.getInstance();
                checkInCalender.setTime(checkInDate);
                OurDate ourCheckIn = new OurDate(checkInCalender.get(Calendar.DAY_OF_MONTH),
                        checkInCalender.get(Calendar.MONTH) + 1, checkInCalender.get(Calendar.YEAR));

                Date checkOutDate = checkOut.getDate();
                Calendar checkOutCalender = Calendar.getInstance();
                checkOutCalender.setTime(checkOutDate);
                OurDate ourCheckOut = new OurDate(checkOutCalender.get(Calendar.DAY_OF_MONTH),
                        checkOutCalender.get(Calendar.MONTH) + 1, checkOutCalender.get(Calendar.YEAR));
                int newAdults = adults.getCount();
                int newChildren = children.getCount();

                if (ourCheckIn.equals(oldCheckInDate) && ourCheckOut.equals(oldCheckOutDate) && newAdults == oldAdults
                        && newChildren == oldChildren) {
                    throw new AccessAppException("No changes were made");
                }
                if (ourCheckIn.equals(ourCheckOut)) {
                    throw new AccessAppException("Check-out date must be after check-in date");
                }
                Guest user = (Guest) getUser();
                Reservation oldReservation = user.getReservations()
                        .remove(roomNumber + email + oldCheckInDate.toString());

                double price = Hotel.getRooms().get(roomNumber).getRoomPrice();
                double newTotalCost = (price + 0.2 * newAdults * price + 0.15 * newChildren * price);
                totalCost.setText(String.valueOf(newTotalCost));

                Reservation reservation = new Reservation(roomNumber, email, ourCheckIn, ourCheckOut, newAdults,
                        newChildren, phoneNumber.getText(), null, newTotalCost);

                user.getReservations().put(roomNumber + email + ourCheckIn.toString(), reservation);
                handleUpdates("Guest", roomNumber, email, "Reservations", reservation, oldReservation);

                reqTable.setValueAt(ourCheckIn, row, 3);
                reqTable.setValueAt(ourCheckOut, row, 4);
                reqTable.setValueAt(newAdults, row, 5);
                reqTable.setValueAt(newChildren, row, 6);
                reqTable.setValueAt(phoneNumber.getText(), row, 2);
                reqTable.setValueAt(newTotalCost, row, 7);

                checkIn.setDate(null);
                checkOut.setDate(null);
                adults.setCounter(0);
                children.setCounter(0);
                phoneNumber.setText("");

                msg.displayMessage(Message.MessageType.SUCCESS, "Reservation updated successfully", bg, layout);
            } catch (AccessAppException ex) {
                msg.displayMessage(Message.MessageType.ERROR, ex.getMessage(), bg, layout);
            }
        });
    }

    public static void initRoomTable(RoomType type, Table table) {
        for (Room room : Hotel.getRooms().values()) {
            if (room.getRoomType().equals(type)) {
                table.addRow(new Object[] { room.getRoomNumber(), room.getRoomPrice(), room.availability() });
            }
        }
    }

    public static void addRoom(OurButton btn, RoomType type, MyTextField price, JCheckBox available, Message msg,
            JPanel bg, MigLayout layout, Table table) {
        btn.addActionListener(e -> {
            boolean isAvailable = available.isSelected();
            try {
                if (price.getText().isEmpty() || !price.getText().matches("[0-9]+(\\.[0-9]+)*")) {
                    throw new AccessAppException("Price is required, and must be a  number!");
                }
                double roomPrice = Double.parseDouble(price.getText());
                Room rm = Manager.addRoom(type, isAvailable, roomPrice);
                table.addRow(new Object[] { rm.getRoomNumber(), rm.getRoomPrice(), rm.availability() });
                msg.displayMessage(Message.MessageType.SUCCESS, "Room added successfully", bg, layout);
                if (managerGui != null && !(Hotel.getUser() instanceof Receptionist))
                    managerGui.getWelcomePage().updateCard("Rooms", Hotel.getRooms().size());
            } catch (AccessAppException ex) {
                msg.displayMessage(Message.MessageType.ERROR, ex.getMessage(), bg, layout);
            }
        });
    }

    public static void deleteRoom(OurButton btn, Table table, Message msg, JPanel bg, MigLayout layout) {
        btn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                return;
            }
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            String roomNumber = model.getValueAt(row, 0).toString();
            if (JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to delete the room with the number " + roomNumber + " ?", "WARNING",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                table.deleteRow(row);
                Manager.removeRoomFromDataBase(roomNumber);
                msg.displayMessage(Message.MessageType.SUCCESS, "Room deleted successfully", bg, layout);
                if (managerGui != null && !(Hotel.getUser() instanceof Receptionist))
                    managerGui.getWelcomePage().updateCard("Rooms", Hotel.getRooms().size());
            }
        });
    }

    public static void updateRoom(OurButton btn, MyTextField priceInput, JCheckBox availableBox, Table table,
            Message msg, JPanel bg, MigLayout layout) {
        btn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                return;
            }
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            String roomNumber = model.getValueAt(row, 0).toString();
            Double roomPriceInTable = (Double) model.getValueAt(row, 1);
            Boolean isAvailableInTable = (Boolean) model.getValueAt(row, 2);
            String price = priceInput.getText();
            double roomPrice = Double.parseDouble(price);
            boolean isAvailable = availableBox.isSelected();
            if (roomPrice == roomPriceInTable && isAvailable == isAvailableInTable) {
                return;
            }
            try {
                if (price.isEmpty() || !price.matches("[0-9]+(\\.[0-9]+)*")) {
                    throw new AccessAppException("Price is required, and must be a number!");
                }

                handleUpdates("Room", roomNumber, null, "price", roomPrice, null);
                handleUpdates("Room", roomNumber, null, "isAvailable", isAvailable, null);
                model.setValueAt(roomPrice, row, 1);
                model.setValueAt(isAvailable, row, 2);
                msg.displayMessage(Message.MessageType.SUCCESS, "Room updated successfully", bg, layout);
            } catch (AccessAppException ex) {
                msg.displayMessage(Message.MessageType.ERROR, ex.getMessage(), bg, layout);
            }
        });
    }

    public static void handleUpdates(String entity, String roomNumber, String email, String key, Object updatedValue,
            Reservation withOldCheckIn) throws AccessAppException {

        switch (entity) {
            case "Guest" -> {
                try {
                    if (!Objects.equals(key, "Reservations")) {
                        Database.updateFieldInDataBase("Guests", "email", email, key, (String) updatedValue);
                    }
                    if (Objects.equals(key, "Reservations")) {
                        if (withOldCheckIn != null) {
                            Database.removeReservationFromGuest(email, withOldCheckIn.getReservationId());
                            Database.addReservationToUser(email, (Reservation) updatedValue);
                            return;
                        }
                        Database.removeReservationFromGuest(email,
                                roomNumber + email + ((Reservation) updatedValue).getCheckInDate().getDay() + "/"
                                        + ((Reservation) updatedValue).getCheckInDate().getMonth() + "/"
                                        + ((Reservation) updatedValue).getCheckInDate().getYear());
                        Database.addReservationToUser(email, (Reservation) updatedValue);
                    }
                } catch (Exception e) {
                    throw new AccessAppException(e.getMessage());
                }
            }
            case "Worker" -> {
                try {
                    if (Objects.equals(key, "email")) {
                        if (!isValidEmail((String) updatedValue))
                            throw new AccessAppException("Invalid Email");
                        if (getUserFromModel("Workers", "OasisMail", (String) updatedValue) != null
                                || getUserFromModel("Workers", "OasisMail", (String) updatedValue) != null)
                            throw new AccessAppException("Email already exists");
                        Database.updateFieldInDataBase("Workers", "OasisMail", email, key, (String) updatedValue);
                    }
                    if (!Objects.equals(key, "Reservations")) {
                        Database.updateFieldInDataBase("Workers", "OasisMail", email, key, (String) updatedValue);
                    }
                    // if (Objects.equals(key, "Reservations")){
                    // HashMap<String, Object> objectHashMap = new HashMap<>((HashMap<String,
                    // Object>) updatedValue);
                    // Document tmpDocument = new Document(objectHashMap);
                    // Database.updateFieldInDataBase("Workers", "OasisMail", email, key,
                    // tmpDocument.toJson());
                    // }
                } catch (Exception e) {
                    throw new AccessAppException(e.getMessage());
                }
            }
            case "Room" -> {
                try {
                    if (key.equals("roomNumber")) {
                        Database.updateFieldInDataBase("Rooms", "roomNumber", roomNumber, key, (String) updatedValue);
                    }
                    if (key.equals("roomType")) {
                        Database.updateFieldInDataBase("Rooms", "roomNumber", roomNumber, key, (String) updatedValue);
                    }
                    if (key.equals("price")) {
                        Database.updateFieldInDataBase("Rooms", "roomNumber", roomNumber, key, (double) updatedValue);
                    }
                    if (key.equals("isAvailable")) {
                        Database.updateFieldInDataBase("Rooms", "roomNumber", roomNumber, key, (boolean) updatedValue);
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
            default -> throw new AccessAppException("Unexpected value: " + entity);
        }
    }

    /**
     * This method is used to convert the rooms in the hotel to a format that can be
     * used in the UI in the GuestUi class
     * 
     * @return HashMap<String,RoomOnList> roomsUiList
     */
    public static HashMap<String, RoomOnList> roomsToRoomPanelGuest() {
        HashMap<String, Room> allRooms = Hotel.getRooms();
        HashMap<String, RoomOnList> roomsUiList = new HashMap<>();

        String description = "";
        int counter;

        if (allRooms.isEmpty()) {
            return roomsUiList;
        }

        for (RoomType roomType : RoomType.values()) {
            description = switch (roomType) {
                case Standard -> "-Single Room with a single bed.";
                case Double -> "-Double Room with a double bed.";
                case Suite -> "-Suite Room with a double bed and a living room.";
                case Family -> "-Family Room with a double bed and two single beds.";
            };

            HashMap<Double, Integer> roomPriceCount = new HashMap<>();
            HashSet<Double> prices = new HashSet<>();
            for (Room room : allRooms.values()) {
                if (room.getRoomType().equals(roomType) && !prices.contains(room.getRoomPrice())) {
                    prices.add(room.getRoomPrice());
                }
            }
            counter = 0;
            for (Double price : prices) {
                for (Room room : allRooms.values()) {
                    if (room.getRoomType().equals(roomType) && room.getRoomPrice() == price && room.availability()) {
                        counter++;
                    }
                }
                roomPriceCount.put(price, counter);
            }
            for (Double price : roomPriceCount.keySet()) {
                LinkedList<String> roomNumbers = new LinkedList<>();
                for (Room r : allRooms.values()) {
                    if (r.getRoomType().equals(roomType) && r.getRoomPrice() == price)
                        roomNumbers.add(r.getRoomNumber());
                }
                RoomOnList roomOnList = new RoomOnList(roomType,
                        "hotelproject/src/main/java/view/icons/" + roomType.toString() + "Room.png",
                        description,
                        price,
                        roomPriceCount.get(price), roomNumbers, null);
                roomsUiList.put(roomType.toString() + price, roomOnList);
            }
        }
        return roomsUiList;
    }

    public static void openBookingUI(OurButton bookButton, LinkedList<String> roomNumbers,
            LinkedList<String> usedRoomNumbers, double price,
            CounterPanel AdultsCounter, CounterPanel ChildrenCounter,
            JXDatePicker checkIn, JXDatePicker checkOut, JTextField creditCardField,
            JTextField phoneNumberField, RoomOnList roomOnList, RoomUI roomUi, Message msg, JPanel bg,
            MigLayout layout) {

        bookButton.addActionListener(e -> {
            User user = getUser();
            if (user instanceof Worker) {
                msg.displayMessage(Message.MessageType.ERROR, "You can't book a room as a worker", bg, layout);
                return;
            }
            if (roomNumbers.isEmpty()) {
                msg.displayMessage(Message.MessageType.ERROR, "No available rooms", bg, layout);
                return;
            }
            int adults = AdultsCounter.getCount();
            int children = ChildrenCounter.getCount();
            String creditCard = creditCardField.getText();
            String phoneNumber = phoneNumberField.getText();
            if (checkIn.getDate() == null || checkOut.getDate() == null || creditCardField.getText().isEmpty()
                    || phoneNumberField.getText().isEmpty()) {
                msg.displayMessage(Message.MessageType.ERROR, "Please fill all the data", bg, layout);
                return;
            }
            if (checkIn.getDate().compareTo(checkOut.getDate()) >= 0) {
                msg.displayMessage(Message.MessageType.ERROR, "Check-out date must be after check-in date", bg, layout);
                return;
            }
            if (creditCard.length() != 16 || !creditCard.matches("[0-9]+")) {
                msg.displayMessage(Message.MessageType.ERROR, "Invalid credit card number", bg, layout);
                return;
            }
            if (phoneNumber.length() != 10 || !phoneNumber.matches("[0-9]+")) {
                msg.displayMessage(Message.MessageType.ERROR, "Invalid phone number", bg, layout);
                return;
            }

            double CalculatedPrice = (price + 0.2 * adults * price + 0.15 * children * price);
            int response = JOptionPane.showConfirmDialog(null,
                    "The price is " + CalculatedPrice + " DZD/Night. Do you want to confirm the booking?",
                    "Confirm Booking", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (response == JOptionPane.YES_OPTION) {

                Date checkInDate = checkIn.getDate();
                Calendar checkInCalender = Calendar.getInstance();
                checkInCalender.setTime(checkInDate);
                OurDate OurCheckIn = new OurDate(checkInCalender.get(Calendar.DAY_OF_MONTH),
                        checkInCalender.get(Calendar.MONTH) + 1, checkInCalender.get(Calendar.YEAR));

                Date checkOutDate = checkOut.getDate();
                Calendar checkOutCalender = Calendar.getInstance();
                checkOutCalender.setTime(checkOutDate);
                OurDate OurCheckOut = new OurDate(checkOutCalender.get(Calendar.DAY_OF_MONTH),
                        checkOutCalender.get(Calendar.MONTH) + 1, checkOutCalender.get(Calendar.YEAR));

                Reservation reservation = new Reservation(roomNumbers.getFirst(), user.getEmail(), OurCheckIn,
                        OurCheckOut, adults, children, phoneNumber, creditCard, CalculatedPrice);
                ;
                ((Guest) user).getReservations().put(reservation.getReservationId(), reservation);
                Database.addReservationToUser(user.getEmail(), reservation);

                Table[] tables = guestUi.getReservationPanel().getTables();
                tables[0].addRow(new Object[] { roomNumbers.getFirst(), user.getEmail(), phoneNumber, OurCheckIn,
                        OurCheckOut, adults, children, CalculatedPrice, false, false });

                String removedRoomNumber = roomNumbers.removeFirst();
                Hotel.getRooms().get(removedRoomNumber).setAvailable(false);
                roomOnList.getRoomNumbers().remove(removedRoomNumber);
                roomOnList.getUsedRoomNumbers().add(removedRoomNumber);
                try {
                    handleUpdates("Room", removedRoomNumber, null, "isAvailable", false, null);
                } catch (AccessAppException ex) {
                    msg.displayMessage(Message.MessageType.ERROR, "Access app error!", bg, layout);
                }
                usedRoomNumbers.add(removedRoomNumber);
                if (roomNumbers.isEmpty()
                        || roomNumbers.stream().noneMatch(val -> Hotel.getRooms().get(val).availability())) {
                    roomOnList.setAvailable(false);
                    roomOnList.repaint();
                    roomOnList.revalidate();
                    roomUi.setAvailable(false);
                    guestUi.revalidate();
                }

                msg.displayMessage(Message.MessageType.SUCCESS, "Request is sent", bg, layout);

                // QrCode qrCode = new QrCode();
                //
                // // User information
                // String username = user.getFirstName() + user.getLastName() ;
                // String email = user.getEmail();
                //
                //
                // // Encode user information into a string
                // String userInfo = "Username: " + username + "\nEmail: " + email + "\nPhone: "
                // + phoneNumber;
                //
                // // Path to save the QR code image
                // String filePath = "user_qr_code.png";
                //
                //// Generate QR code that has these informations
                // qrCode.generateQRCode(username, email, phoneNumber, filePath);
                // try{
                // System.out.println("The QR code has been saved to: " + filePath);
                // SendEmail emailService = new SendEmail();
                // emailService.setupServerProperties();
                // emailService.draftEmail(email , "Here is your QR code: Please present this QR
                // code when checking in at the hotel." + "\n" + "Thank you!" + "\n" + "Best
                // regards," + "\n" + "Hotel Management" + "\n", "");
                // emailService.sendEmailWithAttachment(email,"Oasis QrCode" ,filePath );
                // }catch (MessagingException | IOException exception){
                // exception.printStackTrace();
                // }

                System.out.println("Booking confirmed");
            } else {
                System.out.println("Booking cancelled");
            }
        });
    }

    /**
     * Sets the active panel in the main content area.
     *
     * @param panel            the panel to be displayed
     * @param mainContentPanel the main content panel that holds the shown panels
     */
    public static void setActivePanel(JPanel mainContentPanel, JComponent panel) {
        mainContentPanel.removeAll(); // Remove all existing components from the main content panel
        mainContentPanel.add(panel); // Add the new panel
        mainContentPanel.repaint(); // Repaint the main content panel
        mainContentPanel.revalidate(); // Revalidate the layout of the main content panel
    }

    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:gmail\\.com|yahoo\\.com|yahoo\\.fr|hotmail\\.com|hotmail\\.fr|hotmail\\.co\\.uk|aol\\.com|etu-usthb\\.dz)$";
    private static final Pattern pattern = Pattern.compile(EMAIL_REGEX);

    public static boolean isValidEmail(String email) {
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static User getUser() {
        return Hotel.getUser();
    }
}

class AccessAppException extends Exception {
    public AccessAppException(String message) {
        super(message);
    }
}