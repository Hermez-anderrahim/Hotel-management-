package view.components.roomComponents;

import model.hotel.RoomType;
import net.miginfocom.swing.MigLayout;
import view.components.OurButton;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

public class RoomOnList extends JPanel implements ActionListener {

    private RoomUI roomDetail;

    public RoomUI getRoomDetail() {
        return roomDetail;
    }

    private RoomType roomType;
    private double price;
    private String roomPicture;
    private String roomDescription;
    private JLabel isAvailableLabel;
    private OurButton bookButton=new OurButton("Book now");
    private LinkedList<String> roomNumbers;
    private final LinkedList<String> usedRoomNumbers=new LinkedList<>();

    public double getPrice() {
        return price;
    }

    public JLabel getIsAvailableLabel() {
        return isAvailableLabel;
    }

    public OurButton getBookButton() {
        return bookButton;
    }

    public RoomOnList(RoomType roomType, String roomPicture, String roomDescription, double price, Integer available, LinkedList<String> roomNumber, LinkedList<String> usedRoomNumbers){

        this.roomType = roomType;
        this.roomPicture = roomPicture;
        this.roomDescription = roomDescription;
        this.price=price;
        this.roomNumbers = roomNumber;
        Border border= BorderFactory.createLineBorder(new Color(0xC1A200),2);
        setBorder(border);
        setBackground(new Color(242, 242, 242));

        setLayout(new MigLayout("wrap 2, center, insets 0 5 0 5,gap 5% 5%","[][]","[grow,fill]"));

        ImageIcon icon =new ImageIcon(roomPicture);
        icon = new ImageIcon(icon.getImage().getScaledInstance(200, 150, Image.SCALE_SMOOTH));
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(icon);
        add(imageLabel,"cell 0 0");

        JPanel roomInfo = new JPanel(new MigLayout("wrap 2,inset 5 15 5 20"));
        roomInfo.setBackground(new Color(242, 242, 242));
        add(roomInfo,"center,cell 1 0");

        /////////// RoomOnList Info ///////////
        JLabel roomTypeLabel = new JLabel(roomType.toString());
        roomTypeLabel.setFont(new Font("Lucida Handwriting",Font.PLAIN,22));
        roomInfo.add(roomTypeLabel,"span 2,left,wrap");

        isAvailableLabel = new JLabel();
        isAvailableLabel.setFont(new Font("Inter",Font.PLAIN,15));
        if (available>0) {
            isAvailableLabel.setText("Available");
            isAvailableLabel.setForeground(new Color(0x00A000));
            bookButton.setEnabled(true);
        } else {
            isAvailableLabel.setText("Not Available");
            isAvailableLabel.setForeground(new Color(0xA00000));
            bookButton.setEnabled(false);
        }

        roomInfo.add(isAvailableLabel,"span 2,left,wrap");

        JLabel roomDescriptionLabel = new JLabel("<html>" +roomDescription+"<html>");
        roomDescriptionLabel.setFont(new Font("Inter",Font.PLAIN,10));
        roomInfo.add(roomDescriptionLabel,"span 2,left,growy, pushy, w 60% ,wrap");

        JPanel pricePanel = new JPanel(new MigLayout("wrap 1,filly,inset 0","[grow]","[]2[]"));
        pricePanel.setBackground(new Color(242, 242, 242));
        roomInfo.add(pricePanel,"left");
        JLabel priceLabel = new JLabel(" Price: "+price+"DZD/Night");
        priceLabel.setFont(new Font("Inter",Font.PLAIN,13));
        pricePanel.add(priceLabel,"left,wrap");

        bookButton = new OurButton("Book now");
        bookButton.setButtonBgColor(new Color(0x0377FF));
        pricePanel.add(bookButton,"span 2,left,wrap,growx,pushx");

        bookButton.addActionListener(this);
        /////////////////////////////////
    }

    RoomType getRoomType() {
        return this.roomType;
    };

    public double getRoomPrice() {
        return price;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }
    public String getRoomPicture() {
        return this.roomPicture;
    }
    public void setRoomPicture(String roomPicture) {
        this.roomPicture = roomPicture;
    }
    public String getRoomDescription() {
        return this.roomDescription;
    }
    public void setRoomDescription(String roomDescription) {
        this.roomDescription = roomDescription;
    }

    public void setAvailable(boolean available){
        if(available){
            isAvailableLabel.setText("Available");
            isAvailableLabel.setForeground(new Color(0x00A000));
            bookButton.setEnabled(true);
        }else{
            isAvailableLabel.setText("Not Available");
            isAvailableLabel.setForeground(new Color(0xA00000));
            bookButton.setEnabled(false);
        }
    }

    ///this action is to display the room detail panel and hide(remove) the rooms ui
    @Override
    public void actionPerformed(ActionEvent e) {

        roomDetail = new RoomUI(this);
        //get the parent that is the roomsPanel
        JPanel rooms = (JPanel) getComponent(0).getParent().getParent(); // Assuming RoomsPanel is the parent of Roomr
        // remove all other RoomOnList panels
        rooms.removeAll();

        roomDetail.setVisible(true);
        rooms.add(roomDetail);

        // Revalidate and repaint the RoomsPanel for layout updates
        rooms.revalidate();
        rooms.repaint();

    }

    public LinkedList<String> getRoomNumbers() {
        return roomNumbers;
    }

    public LinkedList<String> getUsedRoomNumbers() {
        return usedRoomNumbers;
    }

    public void addActionListener(ActionListener actionListener) {
    }
}
