/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.belepteto_mvn;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class adatbazis {

    String valaszto = ";";
    String valto;
    private Connection con;
    private Statement st;
    private ResultSet rs;
    public String idoegyseg, nev = "", ora = "", szak = "", oszta = "";
    public Boolean valt = false, van = false, me = false;
    String mentes = "";
    ArrayList<String> nevek = new ArrayList<>();
    ArrayList<String> rfidk = new ArrayList<>();
    ArrayList<log> logg = new ArrayList<>();
    indit inn = new indit();

    public adatbazis() {
        inn.startConnection();
    }

    public void nev_rfid(String sql) {
        try {
            // String kod1="use suliadtb;";
            //    rs=st.executeQuery(kod1);

            String kod = sql;
            rs = inn.le(kod);
            while (rs.next()) {
                String rfi = rs.getString("rfid");
                nev = rs.getString("nev");
                oszta = rs.getString("osztalyid");
                szak = rs.getString("szak");
                int bool = rs.getInt("bent");
                valt = (bool == 1);
                //   System.out.println(rfi+" "+bool+ " "+valt);
            }
        } catch (Exception ex) {
            System.out.println("nev_rfid: " + ex.toString());
            ex.printStackTrace();
        }
    }

    //ido kiratas
    public void ido() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        idoegyseg = sdf.format(cal.getTime());
        ora = idoegyseg.substring(10, 13).trim();
        // System.out.println( ora); 
    }
    //dátum ellenörző
    String idos = "", idos1 = "", nevee = "", rfidd = "";

    class log {

        String rfid;
        String nev;
        String ido;
        String write;
        String helyzet;

        log(String rfid, String nev, String ido, String helyzet) {
            this.rfid = rfid;
            this.nev = nev;
            this.ido = ido;
            this.helyzet = helyzet;
            write = rfid + valaszto + nev + valaszto + ido + valaszto + helyzet + ",\n";
        }

        log(String rfid, String nev, String ido) {
            this.rfid = rfid;
            this.nev = nev;
            this.ido = ido;
            write = rfid + valaszto + nev + valaszto + ido + ",\n";
        }

    }

    public void add_log(String rfid) {
        logg.add(new log(rfid, nev, idos, valto));
    }

    void mysqlbeolv() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String sql1 = "select * from ido;";
        String jelen = sdf.format(cal.getTime());
        String rfi = "";
        String n = "";
        String idod = "";
        try {
            rs = inn.le(sql1);
            while (rs.next()) {
                rfi = rs.getString("rfid");
                n = rs.getString("nev");
                idos1 = rs.getString("ido");
                idod = idos1.substring(0, 10);
                String helyzet = "";
                try {
                    helyzet = rs.getString("helyzet");
                } catch (Exception e) {
                }
                //System.out.println(idod);
                if (jelen.equals(idod)) {
                    logg.add(new log(rfi, n, idos1, helyzet));
                    mentes += rfi + valaszto + n + valaszto + idos1 + valaszto + helyzet + ",\n";
                }
            }
        } catch (Exception ex) {
            System.out.print(ex.getMessage());
        }

    }
    int db;

    void bentmaradtak() {
        try {
            String kod = "select count(*) from tanar where bent=1;";
            rs = inn.le(kod);
            rs.next();
            db = rs.getInt(1);
            System.out.println("siker " + db);
        } catch (Exception ex) {
            System.out.println("1bentmaradtek: " + ex.toString());
        }

        try {
            String kod = "select rfid,nev,bent from tanar where bent=1;";
            rs = st.executeQuery(kod);
            while (rs.next()) {
                nevek.add(rs.getString("nev").trim());
                rfidk.add(rs.getString("rfid").trim());
            }
        } catch (Exception ex) {
            System.out.println("2bentmaradtek: " + ex.toString());
        }
        for (int i = 1; i < db + 1; i++) {
            try {
                inn.fel("UPDATE tanar SET bent=0 WHERE rfid='" + rfidk.get(i) + "';");
                inn.fel("insert into ido values ( null,'" + rfidk.get(i) + "','" + nevek.get(i) + "','" + idoegyseg + " kint');");
            } catch (Exception e) {
                e.printStackTrace();
            }
            logg.add(new log(rfidk.get(i), nevek.get(i), idoegyseg, "kint"));
            mentes += rfidk.get(i) + valaszto + nevek.get(i) + valaszto + idoegyseg + " kint,\n";
        }
        try {
            String ir = "";

            ir = logg.stream().map((logg1) -> logg1.write).reduce(ir, String::concat);
            try {
                BufferedWriter mentt = new BufferedWriter(new FileWriter("/home/pi/Desktop/mentes/" + idoegyseg.substring(0, 10) + ".txt"));
                mentt.write(ir);
            } catch (Exception e) {
            }
            System.out.println("mentve");
            me = true;
            mentes = "";

        } catch (Exception ex) {
            System.out.println("3bentmaradtek: " + ex.toString());
        }
    }

}
