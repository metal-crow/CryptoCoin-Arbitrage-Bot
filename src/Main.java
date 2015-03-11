import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;


public class Main {

    public static ArrayList<Pair<String,String>> loginInfo=new ArrayList<Pair<String,String>>();
    
    public static void main(String[] args) throws IOException, InterruptedException {
        
        BufferedReader in=new BufferedReader(new FileReader("Config.ini"));
        while(in.ready()){
            loginInfo.add(Pair.with(in.readLine(), in.readLine()));
        }
        
        //find way to run headless
        //no point making unified method because each website has unique tags
        
        //log into circle(usd to btc)
        WebDriver circle = new FirefoxDriver();
        circle.get("https://www.circle.com/signin");
        WebElement login=circle.findElement(By.name("form"));
        login.findElement(By.name("email")).sendKeys(loginInfo.get(0).getValue0());
        login.findElement(By.name("pass")).sendKeys(loginInfo.get(0).getValue1());
        login.submit();
        boolean loggedin=false;
        while(!loggedin){
            loggedin = circle.findElements(By.className("page-overview-actions")).size() > 0;
        }
        System.out.println("logged into circle");
        
        //log in to hitbtc
        WebDriver hitbtc = new FirefoxDriver();
        hitbtc.get("https://hitbtc.com/sso/signin");
        WebElement login2=hitbtc.findElement(By.id("form_index_signin"));
        login2.findElement(By.name("username")).sendKeys(loginInfo.get(1).getValue0());
        login2.findElement(By.name("password")).sendKeys(loginInfo.get(1).getValue1());
        login2.submit();
        boolean loggedin2=false;
        while(!loggedin2){
            loggedin2 = hitbtc.findElements(By.className("menuBar_userSignedIn ")).size() > 0;
        }
        System.out.println("logged into hitbtc");
        
        while(true){
            try{
                //check if the price at circle, minus transaction fees, is greater than the bid price at hitbtc, minus transaction fees
                
                //conversion rate is autoupdated, so no worry about refreshing
                //TODO do i have to get this every time to reget the changed string, or can i just use .gettext on the existing var?
                WebElement circleprice_elem=circle.findElement(By.className("page-overview-rate"));
                String circleprice_string=circleprice_elem.getText();
                
                double circleprice=Double.valueOf(circleprice_string.substring(circleprice_string.indexOf("$")));
                //circle processing fee is 0.029%, rounded up to nearest cent
                circleprice=Math.round(circleprice*102.9)/100.0;
                
                //hitbtc's current selling price is auto updated
                WebElement hitbtcsellingprice_elem=hitbtc.findElement(By.className("form_createOrder__offer"));
                double hitbtcsellingprice=Double.valueOf(hitbtcsellingprice_elem.getText());
                //TODO hitbtc's withdraw fees are a Flat rate $9
                
                //buy!
                if(circleprice<hitbtcsellingprice){
                    //must buy btc rounded to 0.01, otherwise i lose money to hitbtc rounding limits
                
                    //TODO circle requires google authenticator to buy, figure out how to use
                }
                //rate limit myself
                Thread.sleep(5000);
            }catch(StaleElementReferenceException e){
                System.err.println("Stale element");
            }
        }
        
        /*in.close();
        btce.quit();
        bitstamp.quit();*/
    }

}
