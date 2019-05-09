package api;

import amosalexa.services.AccountData;
import api.banking.AccountAPI;
import model.banking.StandingOrder;
import org.junit.Test;

import java.util.Collection;

public class StandingOrderTest {

    @Test
    public void getStandingOrdersTest(){
        Collection<StandingOrder> standingOrders = AccountAPI.getStandingOrdersForAccount(AccountData.ACCOUNT_DEFAULT);
        for(StandingOrder standingOrder : standingOrders){
            System.out.println("Id: " + standingOrder.getStandingOrderId());
            System.out.println("First: " + standingOrder.getFirstExecution());
            System.out.println("Rate: " + standingOrder.getExecutionRateString());
            System.out.println("Status: " + standingOrder.getStatus());
            System.out.println("Amount: " + standingOrder.getAmount());
            System.out.println("Destination: " + standingOrder.getDestinationAccount());
        }
    }

}
