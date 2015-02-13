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
                //ordered from low to high
                //store price/coin, # of coins, and usd cost
                ArrayList<Triplet<Double, Double, Double>> lowestsellers=new ArrayList<Triplet<Double, Double, Double>>();
                
                //ordered from high to low
                ArrayList<Triplet<Double, Double, Double>> highestbuyers= new ArrayList<Triplet<Double, Double, Double>>();
                
                int websitesellat=0;//defaults to 0, btce
                int websitebuyat=0;
                
                //------------btce------------
                
                //first child is the lowest
                List<WebElement> btcesellers=btce.findElements(By.xpath("//div[@id=\"orders-s-list\"]/*/*/*/tr[@class=\"order\"]"));
                for(WebElement seller:btcesellers){
                    //NOTE: using strings is 5x faster than findElement
                    String data=seller.getText();
                    int fspace=data.indexOf(" ");
                    int sspace=data.indexOf(" ", fspace+1);

                    lowestsellers.add(
                            Triplet.with(
                                    Double.valueOf(data.substring(0, fspace)), 
                                    Double.valueOf(data.substring(fspace, sspace)), 
                                    Double.valueOf(data.substring(sspace))
                                    )
                            );
                }
                //element [2] is the amount being sold, [3] is the total cost
                
                //first child is highest
                List<WebElement> btcebuyers=btce.findElements(By.xpath("//div[@id=\"orders-b-list\"]/*/*/*/tr[@class=\"order\"]"));
                for(WebElement buyer:btcebuyers){
                    String data=buyer.getText();
                    int fspace=data.indexOf(" ");
                    int sspace=data.indexOf(" ", fspace+1);
                    highestbuyers.add(
                            Triplet.with(
                                    Double.valueOf(data.substring(0, fspace)), 
                                    Double.valueOf(data.substring(fspace, sspace)), 
                                    Double.valueOf(data.substring(sspace))
                                    )
                            );
                }
                //element [2] is the amount being bought, [3] is the total profit
                
                
                //---------------------------------
                //--------------bitstamp------------
                
                //first child is highest (these people are buying, so we sell to them)
                WebElement bitstamp_highest_buyer=bitstamp.findElement(By.xpath("//tbody[@id=\"bids\"]/tr[1]"));

                double highest_Buyer_price=Double.valueOf(bitstamp_highest_buyer.findElement(By.xpath(".//td[@class=\"price\"]")).getText());
                if(highest_Buyer_price>lowestsellers.get(0).getValue0()){
                    websitesellat=1;//selling website is now bitstamp
                    //since these people are buying for higher, use them
                    List<WebElement> bitstampbuyers=bitstamp.findElements(By.xpath("//tbody[@id=\"bids\"]/tr"));
                    highestbuyers.clear();
                    for(WebElement buyer:bitstampbuyers){
                        String data=buyer.getText();
                        //data could change while being read
                        if(!data.equals("") && data!=null){
                            int fspace=data.indexOf(" ");
                            int sspace=data.indexOf(" ", fspace+1);
                            
                            highestbuyers.add(
                                    Triplet.with(
                                            Double.valueOf(data.substring(sspace)), 
                                            Double.valueOf(data.substring(fspace, sspace)), 
                                            Double.valueOf(data.substring(0, fspace))
                                            )
                                    );
                        }
                    }
                }
                
                //first child is lowest
                WebElement bitstamp_lowest_seller=bitstamp.findElement(By.xpath("//tbody[@id=\"asks\"]/tr[1]"));
                
                double lowest_seller_price=Double.valueOf(bitstamp_lowest_seller.findElement(By.xpath(".//td[@class=\"price\"]")).getText());
                if(lowest_seller_price<lowestsellers.get(0).getValue0()){
                    websitebuyat=1;//buying website is now bitstamp
                    //since these people are selling for lower, use them
                    List<WebElement> bitstampsellers=bitstamp.findElements(By.xpath("//tbody[@id=\"asks\"]/tr"));
                    lowestsellers.clear();
                    for(WebElement seller:bitstampsellers){
                        String data=seller.getText();
                        //data could change while being read
                        if(!data.equals("") && data!=null){
                            int fspace=data.indexOf(" ");
                            int sspace=data.indexOf(" ", fspace+1);
                            
                            lowestsellers.add(
                                    Triplet.with(
                                            Double.valueOf(data.substring(sspace)), 
                                            Double.valueOf(data.substring(fspace, sspace)), 
                                            Double.valueOf(data.substring(0, fspace))
                                            )
                                    );
                        }
                    }
                }
                
                //---------------------------------
                //----------check if arbitrage possible-----------
                int index_to_buy_from=0;
                int index_to_sell_to=0;
                boolean doit=false;
                
                while(index_to_buy_from<lowestsellers.size() && !doit){
                    Triplet<Double, Double, Double> seller=lowestsellers.get(index_to_buy_from);
                    System.out.println(seller.getValue0());
                    //find seller to buy from in p1 who is selling less # of coins than the buyer in p2 who is buying the max # of coins.
                    //they also must be selling at lower $/coin than the buyer is buying at
                    //TODO i can further maxamize profit if i search highest buyers as well for a match
                    if(seller.getValue0()<highestbuyers.get(index_to_sell_to).getValue0() && seller.getValue1()<highestbuyers.get(index_to_sell_to).getValue1()){
                        doit=true;
                    }
                    else{
                        index_to_buy_from++;
                    }
                }
                
                //----------------------------------------
                //perform arbitrage
                DecimalFormat df=new DecimalFormat("#.0000");
                
                /*double how_much_usd_id_buy=0;
                if(websitebuyat==0){
                    String text=lowestseller.getText();
                    how_much_usd_id_buy=Double.valueOf(text.substring(text.lastIndexOf(" ")));
                    System.out.println("Buy $"+df.format(how_much_usd_id_buy)+" from btce");
                }else if(websitebuyat==1){
                    String text=lowestseller.getText();
                    how_much_usd_id_buy=Double.valueOf(text.substring(text.lastIndexOf(" ")));
                    System.out.println("Buy $"+df.format(how_much_usd_id_buy)+" from bitstamp");
                }
                double how_much_usd_id_sell=0;
                if(websitesellat==0){
                    String text=highestbuyer.getText();
                    how_much_usd_id_sell=Double.valueOf(text.substring(text.lastIndexOf(" ")));
                    System.out.println("Sell $"+df.format(how_much_usd_id_sell)+" to btce");
                }
                else if(websitesellat==1){
                    String text=highestbuyer.getText();
                    how_much_usd_id_sell=Double.valueOf(text.substring(0,text.indexOf(" ")));
                    System.out.println("Sell $"+df.format(how_much_usd_id_sell)+" to bitstamp");
                }*/
                
                if(doit){
                    System.out.println("Buy "
                                            +lowestsellers.get(index_to_buy_from).getValue1()+" coins at "
                                            +websitebuyat+" for $"+lowestsellers.get(index_to_buy_from).getValue0()+"/coin "
                                    + " and sell "
                                            +highestbuyers.get(index_to_sell_to).getValue1()+" coins at "
                                            +websitesellat+" for $"+highestbuyers.get(index_to_sell_to).getValue0()+"/coin");
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
