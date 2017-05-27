package assignment.model;


import assignment.util.Config;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import store.db.Database;
import store.db.Storable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Order implements Storable {
    public static final String DB_TABLE_NAME = "orders";
    private static final String DB_INTERSECTION_TABLE_NAME = "order_extra";
    public static final String[] DB_TABLE_COLUMNS = {"id", "start_date", "end_date",
            "pick_up", "drop_off", "client_id", "motorhome_id", "motorhome_price_value",
            "motorhome_mileage_start", "motorhome_mileage_end", "season_id",
            "season_price_modifier", "canceled_price_modifier"};
    public static final String DB_DATE_FORMAT = "yyyy-MM-dd";

    public String id;
    public ObjectProperty<LocalDate> startDate;
    public ObjectProperty<LocalDate> endDate;
    public StringProperty pickUp;
    public StringProperty dropOff;

    public ObjectProperty<Client> client;

    public ObjectProperty<Motorhome> motorhome;
    public DoubleProperty motorhomeValue;
    public IntegerProperty motorhomeMileageStart;
    public IntegerProperty motorhomeMileageEnd;

    public ObjectProperty<Season> season;
    public DoubleProperty seasonModifier;

    public boolean isCanceled;
    public DoubleProperty cancellationModifier;

    public ObservableList<Map.Entry<Extra, Double>> extras = FXCollections.observableArrayList();

    public Order() {
        id = null;
        startDate = new SimpleObjectProperty<>(LocalDate.now());
        endDate = new SimpleObjectProperty<>(LocalDate.now().plusDays(1));
        pickUp = new SimpleStringProperty("");
        dropOff = new SimpleStringProperty("");
        client = new SimpleObjectProperty<>(null);
        motorhome = new SimpleObjectProperty<>(null);
        motorhomeValue = new SimpleDoubleProperty(0.00);
        motorhomeMileageStart = new SimpleIntegerProperty(0);
        motorhomeMileageEnd = new SimpleIntegerProperty(0);
        season = new SimpleObjectProperty<>(null);
        seasonModifier = new SimpleDoubleProperty(0.00);

        isCanceled = false;
        cancellationModifier = new SimpleDoubleProperty(0.00);
    }

    public Order(String id, LocalDate startDate, LocalDate endDate, String pickUp, String dropOff,
                 Client client, Motorhome motorhome, Double motorhomeValue,
                 int motorhomeMileageStart, int motorhomeMileageEnd,
                 Season season, Double seasonModifier, boolean isCanceled, Double cancellationModifier) {
        this.id = id;
        this.startDate = new SimpleObjectProperty<>(startDate);
        this.endDate = new SimpleObjectProperty<>(endDate);
        this.pickUp = new SimpleStringProperty(pickUp);
        this.dropOff = new SimpleStringProperty(dropOff);
        this.client = new SimpleObjectProperty<>(client);
        this.motorhome = new SimpleObjectProperty<>(motorhome);
        this.motorhomeValue = new SimpleDoubleProperty(motorhomeValue);
        this.motorhomeMileageStart = new SimpleIntegerProperty(motorhomeMileageStart);
        this.motorhomeMileageEnd = new SimpleIntegerProperty(motorhomeMileageEnd);
        this.season = new SimpleObjectProperty<>(season);
        this.seasonModifier = new SimpleDoubleProperty(seasonModifier);

        this.isCanceled = isCanceled;
        this.seasonModifier = new SimpleDoubleProperty(seasonModifier);

        if (this.id != null) {
            extras.setAll(Order.dbGetAllExtras(this.id));
        }
    }

    /*
     *  DB integration
     */
    @Override
    public HashMap<String, String> deconstruct() {
        HashMap<String, String> values = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DB_DATE_FORMAT);

        values.put("start_date", startDate.getValue().format(formatter));
        values.put("end_date", endDate.getValue().format(formatter));
        values.put("pick_up", pickUp.getValue());
        values.put("drop_off", dropOff.getValue());
        values.put("client_id", client.getValue().id);
        values.put("motorhome_id", motorhome.getValue().id);
        values.put("motorhome_price_value", motorhomeValue.getValue().toString());
        values.put("motorhome_mileage_start", motorhomeMileageStart.getValue().toString());
        values.put("motorhome_mileage_end", motorhomeMileageEnd.getValue().toString());
        values.put("season_id", season.getValue().id);
        values.put("season_price_modifier", seasonModifier.getValue().toString());

        if (isCanceled) {
            values.put("canceled_price_modifier", cancellationModifier.getValue().toString());
        } else {
            values.put("canceled_price_modifier", null);
        }

        return values;
    }

    public static Order construct(HashMap<String, String> valuesMap) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DB_DATE_FORMAT);

        String id = valuesMap.get("id");

        LocalDate startDate = LocalDate.parse(valuesMap.get("start_date"), formatter);
        LocalDate endDate = LocalDate.parse(valuesMap.get("end_date"), formatter);

        String pickUp = valuesMap.get("pick_up");
        String dropOff = valuesMap.get("drop_off");

        Client client = Client.dbGet(valuesMap.get("client_id"));
        Motorhome motorhome = Motorhome.dbGet(valuesMap.get("motorhome_id"));
        double motorhomeValue = Double.valueOf(valuesMap.get("motorhome_price_value"));
        int motorhomeMileageStart = Integer.valueOf(valuesMap.get("motorhome_mileage_start"));
        int motorhomeMileageEnd = Integer.valueOf(valuesMap.get("motorhome_mileage_end"));
        Season season = Season.dbGet(valuesMap.get("season_id"));
        double seasonModifier = Double.valueOf(valuesMap.get("season_price_modifier"));

        String canceledPriceModifierString = valuesMap.get("canceled_price_modifier");
        boolean isCanceled = false;
        double canceledPriceModifier = 0.00;
        if (canceledPriceModifierString != null) {
            isCanceled = true;
            canceledPriceModifier = Double.valueOf(canceledPriceModifierString);
        }

        return new Order(id, startDate, endDate, pickUp, dropOff,
                client, motorhome, motorhomeValue, motorhomeMileageStart, motorhomeMileageEnd, season, seasonModifier, isCanceled, canceledPriceModifier);
    }

    public void schedulePayment() {
        Payment payment = new Payment(null, generateInvoice(), null);
        Payment.dbInsert(payment);
    }

    public Invoice generateInvoice() {
        int paymentPeriod = Integer.valueOf(Config.getConfig("invoice").getProperty("INVOICE_PAYMENT_PERIOD"));
        Invoice invoice = new Invoice(null, this,
                LocalDate.now(), LocalDate.now().plusDays(paymentPeriod));

        if (Invoice.dbInsert(invoice) == 1) {
            return Invoice.dbGetByOrderID(id);
        }
        return null;
    }

    public boolean hasInvoice() {
        return Invoice.dbExists(id);
    }

    /*
     *  DB helpers
     */
    public static Order dbGet(String orderID) {
        HashMap<String, String> searchQuery = new HashMap<>();
        searchQuery.put("id", orderID);

        try {
            HashMap<String, String> returnValues = Database.getTable(DB_TABLE_NAME)
                    .get(Arrays.asList(DB_TABLE_COLUMNS),
                            searchQuery, new HashMap<>());

            if (returnValues.get("id") != null && returnValues.get("id").equals(orderID)) {
                return Order.construct(returnValues);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Order> dbGetAll() {
        List<Order> result = new ArrayList<>();

        try {
            List<HashMap<String, String>> returnList = Database.getTable(DB_TABLE_NAME)
                    .getAll(Arrays.asList(DB_TABLE_COLUMNS),
                            null, null);

            returnList.forEach((HashMap<String, String> valuesMap) -> {
                result.add(Order.construct(valuesMap));
            });
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }
    }

    public static List<Map.Entry<Extra, Double>> dbGetAllExtras(String orderID) {
        if (orderID == null) {
            throw new IllegalArgumentException("Invalid ID given as argument! [null]");
        }
        List<Map.Entry<Extra, Double>> result = new ArrayList<>();

        HashMap<String, String> searchQuery = new HashMap<>();
        searchQuery.put("order_id", orderID);

        try {
            List<HashMap<String, String>> returnList = Database
                    .getTable(DB_INTERSECTION_TABLE_NAME)
                    .getAll(Arrays.asList("extra_id", "extra_price_value"),
                            searchQuery, null);

            returnList.forEach((HashMap<String, String> valuesMap) -> {
                result.add(new AbstractMap.SimpleEntry<>(
                    Extra.dbGet(valuesMap.get("extra_id")),
                    Double.valueOf(valuesMap.get("extra_price_value")
                )));
            });
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }
    }

    public static int dbInsert(Order order) {
        try {
            return Database.getTable(DB_TABLE_NAME)
                    .insert(order.deconstruct());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static int dbInsertExtra(String orderID, String extraID) {
        if (orderID == null || extraID == null) {
            throw new IllegalArgumentException("Invalid ID given as argument! [null]");
        }
        HashMap<String, String> entry = new HashMap<>();
        entry.put("order_id", orderID);
        entry.put("extra_id", extraID);

        try {
            return Database.getTable(DB_INTERSECTION_TABLE_NAME)
                    .insert(entry);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}