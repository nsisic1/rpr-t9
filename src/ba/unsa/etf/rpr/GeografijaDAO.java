package ba.unsa.etf.rpr;

import java.sql.*;
import java.util.ArrayList;

public class GeografijaDAO {

    private static GeografijaDAO instance = null;

    private static Connection conn;

    private static PreparedStatement nadjiDrzavu;
    private static PreparedStatement nadjiGlavniGradDrzave;
    private static PreparedStatement obrisiDrzavuNeIGradove;
    private static PreparedStatement obrisiGradoveDrzave;
    private static PreparedStatement nadjiGradoveSortBrStanovnikaD;
    private static PreparedStatement unesiGrad;
    private static PreparedStatement unesiDrzavu;
    private static PreparedStatement promijeniGrad;
    private static PreparedStatement promijeniDrzavu;
    private static PreparedStatement nadjiDrzavuPoID;
    private static PreparedStatement nadjiGlavniGradID;
    private static PreparedStatement nadjiGrad;


    private static void initialize() throws SQLException { // TODO: zabiljezi: staticna metoda, kreira intancu
        instance = new GeografijaDAO();
    }

    private GeografijaDAO() throws SQLException {
        conn = null;
        String url = "jdbc:sqlite:baza.db";
        conn = DriverManager.getConnection(url);

        try {
            conn = DriverManager.getConnection(url);
            kreirajPopuniTabele();
            pripremiUpite();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static GeografijaDAO getInstance() {
        try {
            if (instance == null) {
                initialize();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public static void removeInstance() {
        if (conn == null) {
            return; // Izgleda da je potrebno
        }
        try {
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        conn = null;
        instance = null;
    }

    private void kreirajPopuniTabele() throws SQLException {
        PreparedStatement stmt;

        try {
            stmt = conn.prepareStatement("SELECT * FROM grad");
            stmt.executeQuery();
            return;
        } catch (Exception e) {
            //
        }

        try {
            stmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS `drzava` ( `id` INTEGER, `naziv` TEXT, `glavni_grad` INTEGER," +
                    " PRIMARY KEY(`id`), FOREIGN KEY(`glavni_grad`) REFERENCES `grad`(`id`) )");
            stmt.execute(); // todo : zabiljezi
            stmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS \"grad\" ( `id` INTEGER, `naziv` TEXT, `broj_stanovnika` INTEGER," +
                    " `drzava` INTEGER, FOREIGN KEY(`drzava`) REFERENCES `drzava`(`id`), PRIMARY KEY(`id`) )");
            stmt.execute();
            pripremiUpite();
            popuniTabele();
        } catch (Exception e) {
        }
    }


    Grad glavniGrad(String drzava) {
        if (nadjiDrzavu(drzava) == null) {
            return null;
        }
        try {
            nadjiGlavniGradDrzave.setString(1, drzava);
            ResultSet resultSet = nadjiGlavniGradDrzave.executeQuery();

            if (resultSet.next()) { // trebao bi samo jedan
                Grad grad = new Grad();
                grad.setNaziv(resultSet.getString(2));
                grad.setBrojStanovnika(resultSet.getInt(3));
                Drzava d = new Drzava();
                d.setNaziv(drzava);
                d.setGlavniGrad(grad);
                grad.setDrzava(d);
                resultSet.close();
                return grad;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // u slucaju da ne postoji glavni grad ?
    }

    public void obrisiDrzavu(String drzava) {
        try {
            nadjiDrzavu.setString(1, drzava);
            ResultSet resultSet = nadjiDrzavu.executeQuery();

            if (!resultSet.next()) {
                resultSet.close();
                return; // nema drzave
            }
            resultSet.close();
            Drzava d = nadjiDrzavu(drzava);
            try {
                izbrisiGradove(drzava); // Prvo brisemo sve gradove ove drzave
                obrisiDrzavuNeIGradove.setString(1, drzava);
                obrisiDrzavuNeIGradove.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } catch (SQLException e) {
            //
        }
    }

    private void izbrisiGradove(String drzava) {
        try {
            nadjiDrzavu.setString(1, drzava);
            ResultSet resultSet = nadjiDrzavu.executeQuery();
            int drzavaId;
            if (resultSet.next()) {
                drzavaId = resultSet.getInt(1);
            } else {
                resultSet.close();
                return;
            }
            resultSet.close();
            obrisiGradoveDrzave.setInt(1, drzavaId);
            obrisiGradoveDrzave.executeUpdate();
        } catch (SQLException e) {
            //
        }
    }

    public ArrayList<Grad> gradovi() {
        ArrayList<Grad> retval = new ArrayList<Grad>();
        try {
            ResultSet resultSet = nadjiGradoveSortBrStanovnikaD.executeQuery();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            // nadjiGradoveSortBrStanovnikaD = conn.prepareStatement("SELECT grad.id, grad.naziv, broj_stanovnika, grad.drzava, drzava.naziv, glavni_grad FROM grad, drzava WHERE grad.drzava = drzava.id ORDER BY broj_stanovnika DESC");
            // nadjiGradoveSortBrStanovnikaD = conn.prepareStatement("SELECT grad.id, grad.naziv, broj_stanovnika, grad.drzava, drzava.naziv, glavni_grad FROM grad, drzava WHERE grad.drzava = drzava.id ORDER BY broj_stanovnika DESC");
            ResultSet resultSet = nadjiGradoveSortBrStanovnikaD.executeQuery();
            while (resultSet.next()) {
                Grad grad = new Grad();
                Drzava d = new Drzava();
                grad.setNaziv(resultSet.getString(2));
                grad.setBrojStanovnika(resultSet.getInt(3));
                d.setNaziv(resultSet.getString(5));
                // Prolazeci sve gradove u bazi ovom while petljom, sigurno cemo proci i kroz sve gradove koji su ujedno
                // i glavni gradovi svojih drzava, to provjeravamo ovdje*
                if (resultSet.getInt(1) == resultSet.getInt(6)) {
                    d.setGlavniGrad(grad);
                }
                grad.setDrzava(d);
                retval.add(grad);
            }
            resultSet.close();
            return retval;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void dodajGrad(Grad grad) {
        try {
            nadjiGrad.setString(1, grad.getNaziv());
            ResultSet resultSet = nadjiGrad.executeQuery();
            if (resultSet.next()) { // grad vec postoji
                return;
            }

            resultSet.close();
            // Nadjemo id drzave
            int drzavaId = 0;
            if (grad.getDrzava() != null) {
                nadjiDrzavu.setString(1, grad.getDrzava().getNaziv());
                resultSet = nadjiDrzavu.executeQuery();
                if (resultSet.next()) {
                    drzavaId = resultSet.getInt(1);
                } else {
                    return;
                }
                resultSet.close();
            }

            // INSERT OR REPLACE INTO grad(naziv, broj_stanovnika, drzava) VALUES(?, ?, ?)
            unesiGrad.setString(1, grad.getNaziv());
            unesiGrad.setInt(2, grad.getBrojStanovnika());

            if (grad.getDrzava() != null) {
                unesiGrad.setInt(3, drzavaId);
                unesiGrad.execute();
                izmijeniDrzavu(grad.getDrzava());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void dodajDrzavu(Drzava drzava) {
        try {
            unesiDrzavu.setString(1, drzava.getNaziv());
            // trazimo id glavnog grada
            nadjiGlavniGradID.setString(1, drzava.getGlavniGrad().getNaziv()); // drzava jos nije dodana... treba naci ID GLAVNOG GRADA
            ResultSet resultSet = nadjiGlavniGradID.executeQuery();
            int glavniGradID = 0;
            if (resultSet.next()) {
                glavniGradID = resultSet.getInt(1);
                unesiDrzavu.setInt(2, glavniGradID);
            }
            unesiDrzavu.executeUpdate();
        } catch (SQLException e) {
            return;
        }
    }

    public void izmijeniGrad(Grad grad) {
        //             promijeniGrad = conn.prepareStatement("UPDATE grad SET naziv = ?, broj_stanovnika = ?, drzava = ? WHERE id = ?");
        try {
            promijeniGrad.setString(1, grad.getNaziv());
            promijeniGrad.setInt(2, grad.getBrojStanovnika());
            nadjiDrzavu.setString(1, grad.getDrzava().getNaziv());
            ResultSet resultSet = nadjiDrzavu.executeQuery();
            if (resultSet.next()) {
                promijeniGrad.setInt(3, resultSet.getInt(3));
            }
            promijeniGrad.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void izmijeniDrzavu(Drzava drzava) {
        if (drzava.getGlavniGrad() == null) {
            return;
        }
        try {
            promijeniDrzavu.setString(1, drzava.getNaziv());
            // Trazimo ID glavnog grada
            nadjiGlavniGradID.setString(1, drzava.getGlavniGrad().getNaziv());
            ResultSet resultSet = nadjiGlavniGradID.executeQuery();
            int id = 0;
            if (resultSet.next()) {
                id = resultSet.getInt(1);
            }
            promijeniDrzavu.setInt(2, id);
            promijeniDrzavu.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Drzava nadjiDrzavu(String drzava) {
        Drzava d = new Drzava();
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT naziv, glavni_grad FROM drzava WHERE naziv = ?");
            stmt.setString(1, drzava);
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.isClosed()) {
                return null;
            }
            d.setNaziv(resultSet.getString(1));
            return d;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static void pripremiUpite() {
        try {
            nadjiGlavniGradDrzave = conn.prepareStatement("SELECT grad.id, grad.naziv, broj_stanovnika FROM grad, drzava WHERE drzava.naziv = ? AND drzava.glavni_grad = grad.id;"); // TODO prepravi metode, dodao sam id ovdje
            obrisiDrzavuNeIGradove = conn.prepareStatement("DELETE FROM drzava WHERE naziv = ?");
            obrisiGradoveDrzave = conn.prepareStatement("DELETE FROM grad WHERE drzava = ?");
            nadjiGradoveSortBrStanovnikaD = conn.prepareStatement("SELECT grad.id, grad.naziv, broj_stanovnika, grad.drzava, drzava.naziv, glavni_grad FROM grad, drzava WHERE grad.drzava = drzava.id ORDER BY broj_stanovnika DESC");
            unesiGrad = conn.prepareStatement("INSERT OR REPLACE INTO grad(naziv, broj_stanovnika, drzava) VALUES(?, ?, ?)");
            unesiDrzavu = conn.prepareStatement("INSERT OR REPLACE INTO drzava(naziv, glavni_grad) VALUES(?, ?)");
            promijeniGrad = conn.prepareStatement("UPDATE grad SET naziv = ?, broj_stanovnika = ?, drzava = ? WHERE id = ?");
            promijeniDrzavu = conn.prepareStatement("UPDATE drzava SET naziv = ?, glavni_grad = ?");
            nadjiDrzavu = conn.prepareStatement("SELECT id, naziv, glavni_grad FROM drzava WHERE naziv = ?");
            nadjiGlavniGradID = conn.prepareStatement("SELECT id FROM grad WHERE naziv = ?");
            nadjiDrzavuPoID = conn.prepareStatement("SELECT id, naziv, glavni_grad FROM drzava WHERE id = ?");
            nadjiGrad = conn.prepareStatement("SELECT id FROM grad WHERE naziv = ?");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private void popuniTabele() throws SQLException {
        PreparedStatement gradovi = conn.prepareStatement("INSERT INTO grad(naziv, broj_stanovnika, drzava) VALUES " +
                " ('Pariz', 2206488, 1), ('London', 8825000, 2), ('Beč', 1899055, 3), ('Manchester', 545500, 2), ('Graz', 280200, 3);");
        gradovi.execute();

        PreparedStatement drzave = conn.prepareStatement("INSERT INTO drzava(naziv, glavni_grad) VALUES " +
                " ('Francuska', 1), ('Velika Britanija', 2), ('Austrija', 3);");
        drzave.execute();
    }

}


// TODO nakon iteriranja zatvoriti kursor kojim iteriramo kroz resultSet; mada ce se vjerovatno sam zatvoriti
// moze se i desiti da baza ostane zauzeta (neko vrijeme?) ako neki upit nije kako treba
// getGradFromResultSet

// sqlite ne dozvoljava naknadno dodavanje foreign key-eva?
// izbrisao not null restrikcije jer su one mozda uzrokovale "foreign key mismatch"