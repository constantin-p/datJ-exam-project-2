package assignment.core.section;

import assignment.core.RootController;
import assignment.model.AccessType;
import assignment.model.AccountType;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

import java.util.List;

public class AccountTypesController implements UISection {
    private static final String ACCESS_TYPE_NAME = "account_types";
    private static final String TEMPLATE_PATH = "templates/section/account_types.fxml";

    private RootController rootController;
    private ObservableList<AccountType> accountTypeMap = FXCollections.observableArrayList();

    @FXML
    private TableView<AccountType> tableView;

    public AccountTypesController(RootController rootController) {
        this.rootController = rootController;
    }

    @FXML
    public void initialize() {
        TableColumn<AccountType, String> nameColumn = new TableColumn("Name");
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().name);

        TableColumn permissionsColumn = new TableColumn("Permissions");
        tableView.getColumns().addAll(nameColumn, permissionsColumn);
        tableView.setItems(accountTypeMap);

        // Set the permission columns
        List<AccessType> accessTypes = AccessType.dbGetAll();
        accessTypes.forEach(accessType -> {
            TableColumn<AccountType, Boolean> accessTypeColumn = new TableColumn(accessType.name.getValue());
            accessTypeColumn.setCellValueFactory(cellData ->
                    new SimpleBooleanProperty(hasAccess(cellData.getValue(), accessType)));
            accessTypeColumn.setCellFactory(column -> new CheckBoxTableCell());

            permissionsColumn.getColumns().add(accessTypeColumn);
        });

        // Load account types
        List<AccountType> accountTypes = AccountType.dbGetAll();
        accountTypes.forEach(accountType -> {
            accountTypeMap.add(accountType);
        });
    }

    public static String getAccessTypeName() {
        return ACCESS_TYPE_NAME;
    }

    public String getTemplatePath() {
        return TEMPLATE_PATH;
    }

    @FXML
    public void handleAddAction(ActionEvent event) {
        AccountType accountType = rootController.modalDispatcher.showCreateAccountTypeModal(null);
        if (accountType != null) {
            // Load account types
            List<AccountType> accountTypes = AccountType.dbGetAll();
            accountTypeMap.clear();
            accountTypes.forEach(entry -> {
                accountTypeMap.add(entry);
            });
        }
    }

    /*
     *  Helpers
     */
    private boolean hasAccess(AccountType accountType, AccessType accessType) {
        for (AccessType accountAccessType: accountType.permissions) {
            if (accountAccessType.name.getValue().equals(accessType.name.getValue())) {
                return true;
            }
        }
        return false;
    }
}