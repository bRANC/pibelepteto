/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.belepteto_mvn;

import com.fazecast.jSerialComm.SerialPort;
import java.sql.ResultSet;
import javax.swing.SwingWorker;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.JOptionPane;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

/**
 *
 * @author branc
 */
public class prog extends javax.swing.JFrame {

    public static adatbazis dbb;
    SerialPort serialPort;
    read be = new read();

    ArrayList<debounce> deb = new ArrayList<>();

    class debounce {

        Instant start;
        String rfid;

        public debounce(String rfid) {
            this.rfid = rfid;
            start = Instant.now();
        }

        public boolean torolheto() {
            //System.out.println(Duration.between(start, Instant.now()).toMillis());
            return Duration.between(start, Instant.now()).toMillis() >= 6000;//30000 fél perc;
        }
    }

    public void sqlalert(String hibauzenet) {
        JOptionPane.showMessageDialog(this, hibauzenet + "\n" + dbb.inn.hiba());
    }

    public void extalert(String hibauzenet, Exception ex) {
        JOptionPane.showMessageDialog(this, hibauzenet + "\n" + ex.toString());
    }

    public void alert(String uzenet) {
        JOptionPane.showMessageDialog(this, uzenet);
    }

    /**
     * Creates new form prog
     */
    public prog() {
        initComponents();
        pi4jsetup();
        dbb = new adatbazis();
        dbb.mysqlbeolv();
        if (!dbb.inn.conn()) {
            sqlalert("Nem sikerült kapcsolódni az adatbázishoz.");
        }
        dbb.ido();
        try {
            inite();
        } catch (Exception e) {
        }
    }

    // provision gpio pin #01 as an output pin and turn on
    GpioPinDigitalOutput pin1;
    GpioPinDigitalOutput pin2;
    GpioPinDigitalOutput pin3;

    public void pi4jsetup() {
        try {
            System.out.println("<--Pi4J--> GPIO Control Example ... started.");

            // create gpio controller
            final GpioController gpio = GpioFactory.getInstance();

            // provision gpio pin #01 as an output pin and turn on
            pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "Zöld", PinState.LOW);
            pin2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Piros", PinState.LOW);
            pin3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Sárga", PinState.LOW);

            // set shutdown state for this pin
            pin1.setShutdownOptions(true, PinState.LOW);
            pin2.setShutdownOptions(true, PinState.LOW);
            pin3.setShutdownOptions(true, PinState.LOW);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inite() {
        SerialPort[] portok = SerialPort.getCommPorts();
        int i = 0;
        for (SerialPort port : portok) {
            System.out.println(i++ + ": " + port.getSystemPortName());
        }
        boolean next = true;

        int szam = 0;
        while (next) {
            serialPort = portok[szam];//portok

            if (serialPort.openPort()) {
                System.out.println("sikerült kapcsolodni.");
                next = false;
                //Instant start = Instant.now();
                varas(100);
                be.execute();
//                while (Duration.between(start, Instant.now()).toMillis() < 100) {
//                }

            } else {
                System.out.println("nemsikerült.");
                szam++;
                if (szam == portok.length) {
                    next = false;
                    alert("Nem található leolvasó.");
                    System.out.println("nemsikerült és el fogytak a portok");
                }
            }
        }
        serialPort.setComPortParameters(9600, 8, 1, SerialPort.NO_PARITY);
    }

    class read extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {
            while (true) {
                Scanner data = new Scanner(serialPort.getInputStream());
                String val1 = "", rfid = "";
                try {
                    while (data.hasNextLine()) {

                        dbb.ido();
                        jteideo.setText(dbb.idoegyseg);
                        boolean mehet = true;
                        try {
                            rfid = (data.nextLine()).trim();

                            for (int i = 0; i < deb.size(); i++) {
                                if (deb.get(i).rfid.equals(rfid)) {
                                    if (deb.get(i).torolheto()) {
                                        deb.remove(i);
                                    } else {
                                        mehet = false;
                                    }
                                }
                            }

                            //   System.out.println(dbb.ora);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (mehet) {
                            System.out.println("serial: " + rfid);
                            deb.add(new debounce(rfid));

                            if (Integer.parseInt(dbb.ora) > 5 && Integer.parseInt(dbb.ora) < 22) {
                                jterfid.setText(rfid);//jtextfield
                                jtrfid.setText(rfid);//jtextinput
                                dbb.me = false;
                                dbb.nev_rfid("select * from tanar where rfid like \"" + rfid.trim() + "\";");
                                //     dbb.rfidell("select rfid from tanar where rfid='"+val+"';");
                                if (rfid.length() > 6) {
                                    if (!dbb.nev.equals("")) {

                                        serialPort.getOutputStream().write('1');
                                        // serialPort.getOutputStream().write('0');

                                        if (dbb.valt == true) {
                                            // System.out.println(val+"  "+dbb.valt);
                                            dbb.mentes += rfid + " " + dbb.nev + " " + dbb.idoegyseg + " kint,\n";
                                            dbb.valto = "kint";
                                            dbb.add_log(rfid);
                                            try {
                                                dbb.inn.fel("insert into ido values ('" + rfid + "','" + dbb.nev + "','" + dbb.idoegyseg + "');");
                                                dbb.inn.fel("UPDATE tanar SET bent=0 WHERE rfid='" + rfid + "';");
                                            } catch (Exception e) {
                                                pin2.pulse(6000, false);
                                                e.printStackTrace();
                                            }
                                            pin3.pulse(6000, false);
                                        } else {
                                            //  System.out.println(val+"  "+dbb.valt);
                                            dbb.valto = "bent";
                                            dbb.add_log(rfid);
                                            dbb.mentes += rfid + " " + dbb.nev + " " + dbb.idoegyseg + " bent,\n";
                                            try {
                                                dbb.inn.fel("insert into ido values ('" + rfid.trim() + "','" + dbb.nev + "','" + dbb.idoegyseg + "')");
                                                dbb.inn.fel("UPDATE tanar SET bent=1 WHERE rfid='" + rfid.trim() + "';");
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                pin2.pulse(6000, false);
                                            }
                                            pin1.pulse(6000, false);
                                        }

                                        jtenev.setText(dbb.nev);
                                        jteallapot.setText(dbb.valto);
                                        dbb.nev = "";
                                        rfid = "";
                                    } else {
                                        serialPort.getOutputStream().write('2');
                                    }
                                }
                            } else {//
                                rfid = "";
                                if (dbb.me == false) {
                                    dbb.bentmaradtak();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void done() {
            System.out.println("done usercheck");
        }

    }

    void varas(int ido) {
        try {
            Thread.sleep(ido);
        } catch (Exception e) {
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jTabbedPane4 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jteideo = new javax.swing.JLabel();
        jtenev = new javax.swing.JLabel();
        jteallapot = new javax.swing.JLabel();
        jterfid = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jTszak = new javax.swing.JTextField();
        jToszta = new javax.swing.JTextField();
        jTnev = new javax.swing.JTextField();
        jtrfid = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        rfidkeres = new javax.swing.JButton();
        nevkeres = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Beléptető");

        jTabbedPane4.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jTabbedPane4.setToolTipText("");

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jteideo.setText("\"idő\"");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jteideo, gridBagConstraints);

        jtenev.setText("\"név\"");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jtenev, gridBagConstraints);

        jteallapot.setText("\"helyzet\"");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jteallapot, gridBagConstraints);

        jterfid.setText("\"rfid\"");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jterfid, gridBagConstraints);

        jLabel1.setText("Idő:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        jPanel1.add(jLabel1, gridBagConstraints);

        jLabel2.setText("RFID: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(50, 5, 0, 5);
        jPanel1.add(jLabel2, gridBagConstraints);

        jLabel3.setText("Név:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(50, 5, 0, 5);
        jPanel1.add(jLabel3, gridBagConstraints);

        jLabel4.setText("Hely: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(50, 5, 0, 5);
        jPanel1.add(jLabel4, gridBagConstraints);

        jTabbedPane4.addTab("Alap", jPanel1);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jButton1.setText("Felvitel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(63, 53, 10, 10);
        jPanel2.add(jButton1, gridBagConstraints);

        jButton3.setText("Adatok frissitése");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(63, 53, 10, 10);
        jPanel2.add(jButton3, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.ipadx = 250;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 10, 0, 0);
        jPanel2.add(jTszak, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.ipadx = 250;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 10, 5, 5);
        jPanel2.add(jToszta, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.ipadx = 250;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(7, 10, 5, 5);
        jPanel2.add(jTnev, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.ipadx = 250;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 10, 5, 5);
        jPanel2.add(jtrfid, gridBagConstraints);

        jLabel5.setText("RFID");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(11, 10, 0, 0);
        jPanel2.add(jLabel5, gridBagConstraints);

        jLabel6.setText("Név");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 0);
        jPanel2.add(jLabel6, gridBagConstraints);

        jLabel7.setText("Osztály");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 10, 0, 0);
        jPanel2.add(jLabel7, gridBagConstraints);

        jLabel8.setText("Szak");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 10, 0, 0);
        jPanel2.add(jLabel8, gridBagConstraints);

        rfidkeres.setText("keres");
        rfidkeres.setToolTipText("");
        rfidkeres.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rfidkeresActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 34, 5, 5);
        jPanel2.add(rfidkeres, gridBagConstraints);

        nevkeres.setText("keres");
        nevkeres.setToolTipText("");
        nevkeres.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nevkeresActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 34, 5, 5);
        jPanel2.add(nevkeres, gridBagConstraints);

        jTabbedPane4.addTab("Managment", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 412, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jTabbedPane4, javax.swing.GroupLayout.Alignment.TRAILING))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 348, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jTabbedPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 348, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            dbb.inn.fel("insert into tanar values ('" + jtrfid.getText().trim() + "' ,'" + jTnev.getText().trim() + "','" + jToszta.getText().trim() + "','" + jTszak.getText().trim() + "','','0');");
        } catch (Exception e) {
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        try {
            dbb.inn.fel("UPDATE tanar SET osztalyid='" + jToszta.getText().trim() + "' , szak='" + jTszak.getText().trim() + "' , nev='" + jTnev.getText() + "'  WHERE rfid='" + jtrfid.getText().trim() + "';");
        } catch (Exception e) {
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void rfidkeresActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rfidkeresActionPerformed

        dbb.nev_rfid("Select * from tanar where rfid='" + jtrfid.getText().trim() + "';");
        jTnev.setText(dbb.nev);
        jToszta.setText(dbb.oszta);
        jTszak.setText(dbb.szak);

    }//GEN-LAST:event_rfidkeresActionPerformed

    private void nevkeresActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nevkeresActionPerformed
        try {
            ResultSet rs = dbb.inn.le("Select * from tanar where nev='" + jTnev.getText().trim() + "';");
            while (rs.next()) {
                jtrfid.setText(rs.getString("nev"));
                jToszta.setText(rs.getString("osztalyid"));
                jTszak.setText(rs.getString("szak"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_nevkeresActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(prog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(prog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(prog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(prog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new prog().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTabbedPane jTabbedPane4;
    private javax.swing.JTextField jTnev;
    private javax.swing.JTextField jToszta;
    private javax.swing.JTextField jTszak;
    public javax.swing.JLabel jteallapot;
    public javax.swing.JLabel jteideo;
    public javax.swing.JLabel jtenev;
    public javax.swing.JLabel jterfid;
    public javax.swing.JTextField jtrfid;
    private javax.swing.JButton nevkeres;
    private javax.swing.JButton rfidkeres;
    // End of variables declaration//GEN-END:variables
}
