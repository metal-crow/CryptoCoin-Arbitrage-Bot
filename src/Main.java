import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.javatuples.Pair;
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
        
        //log into btce
        WebDriver btce = new FirefoxDriver();
        btce.get("https://btc-e.com");
        WebElement login=btce.findElement(By.id("login"));
        login.findElement(By.id("email")).sendKeys(loginInfo.get(0).getValue0());
        login.findElement(By.id("password")).sendKeys(loginInfo.get(0).getValue1());
        login.submit();
        boolean loggedin=false;
        while(!loggedin){
            loggedin = btce.findElements(By.className("profile")).size() > 0;
        }
        System.out.println("logged in btce");
        
        //log in to btistamp
        WebDriver bitstamp = new FirefoxDriver();
        bitstamp.get("https://www.bitstamp.net/account/login/");
        WebElement login2=bitstamp.findElement(By.id("login_form"));
        login2.findElement(By.id("id_username")).sendKeys(loginInfo.get(1).getValue0());
        login2.findElement(By.id("id_password")).sendKeys(loginInfo.get(1).getValue1());
        login2.submit();
        boolean loggedin2=false;
        while(!loggedin2){
            loggedin2 = bitstamp.findElement(By.xpath("//div[@class=\"container\"]/div[@class=\"right\"]/ul")).getText().contains("LOGOUT");
        }
        bitstamp.get("https://www.bitstamp.net/market/tradeview/");
        System.out.println("logged in bitstamp");
        
        while(true){
            try{
                double lowestsellprice=Double.MAX_VALUE;
                WebElement lowestsell;
                
                double highestbuyprice=0;
                WebElement highestbuy;
                
                int websitesellat=0;//defaults to 0, btce
                int websitebuyat=0;
                
                //btce
                //first child is the lowest
                lowestsell=btce.findElement(By.xpath("//div[@id=\"orders-s-list\"]/*/*/*/tr[@class=\"order\"][1]"));
                //element [2] is the amount being sold, [3] is the total cost
                
                //first child is highest
                highestbuy=btce.findElement(By.xpath("//div[@id=\"orders-b-list\"]/*/*/*/tr[@class=\"order\"][1]"));
                //element [2] is the amount being bought, [3] is the total profit
                
                //we check this first, so they are always stored
                lowestsellprice=Double.valueOf(lowestsell.findElement(By.xpath("td[1]")).getText());
                highestbuyprice=Double.valueOf(highestbuy.findElement(By.xpath("td[1]")).getText());
                
                
                //bitstamp
                //first child is highest (these people are buying, so we sell to them)
                WebElement bitstampbuy=bitstamp.findElement(By.xpath("//tbody[@id=\"bids\"]/tr[1]"));
                double tmp=Double.valueOf(bitstampbuy.findElement(By.xpath("//td[@class=\"price\"]")).getText());
                if(tmp>highestbuyprice){
                    highestbuyprice=tmp;
                    highestbuy=bitstampbuy;
                    websitesellat=1;//buying website is now bitstamp
                }
                //first child is lowest
                WebElement bitstampsells=bitstamp.findElement(By.xpath("//tbody[@id=\"asks\"]/tr[1]"));
                double tmp1=Double.valueOf(bitstampsells.findElement(By.xpath("//td[@class=\"price\"]")).getText());
                if(tmp1<lowestsellprice){
                    lowestsellprice=tmp1;
                    lowestsell=bitstampsells;
                    websitebuyat=1;//selling website is now bitstamp
                }
                
                if(lowestsellprice<highestbuyprice){
                    System.out.println("Buy at "+websitebuyat+" for "+lowestsellprice+" and sell at "+websitesellat+" for "+highestbuyprice);
                }
                //System.out.println(lowestsellprice+" "+highestbuyprice);
                
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
