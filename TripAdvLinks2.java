import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.*;
import java.util.regex.*;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.String;
public class TripAdvLinksv2 {
	//deklaracja tablic do przechowywania adresów 
    public static ArrayList<String> mainCategories= new ArrayList<String>();
    public static ArrayList<String> mainClasses= new ArrayList<String>();
    public static ArrayList<String> mainReviews= new ArrayList<String>();
    public static ArrayList<String> pagesCategories= new ArrayList<String>();
    public static ArrayList<String> pagesClasses= new ArrayList<String>();
    public static ArrayList<String> pagesReviews= new ArrayList<String>();
	public static ArrayList<String> fullComments = new ArrayList<String>();
	public static ArrayList<String> commentsLinks = new ArrayList<String>();
	//zmienna określająca etap algorytmu
	public static int stage=1;	
	//zapis danych do pliku
    public static void write(String filename, ArrayList<String> urls) throws IOException{
        BufferedWriter outputWriter = null;
        outputWriter = new BufferedWriter(new FileWriter(filename));
        for (int i = 0; i < urls.size(); i++) {
            	outputWriter.write(urls.get(i));
		outputWriter.newLine();
        }
        outputWriter.flush();
		outputWriter.close();
    }
	//metoda do dodawania nastepnych stron do odpowiedniej tablicy
    public static void addNextPages(Set<String> pages,int inception){
    	ArrayList<String> nextUrl = new ArrayList<String>();
		ArrayList<String> pagesList = new ArrayList<String>(pages);
		int tmpValue=0;int maxValue=0;String[] parts =new String[10];
		String first = pagesList.get(0);int scale = 30;int sizeOfPages = pagesList.size();String splitter = "-o";
		Pattern pattern = Pattern.compile("^.*[o][a-r]([0-9]{2,4}).*$"); 
		switch(inception) {
			case 1: pattern = Pattern.compile("^.*[o][a]([0-9]{2,4}).*$");
				parts = first.split("-oa[0-9]{2,4}");splitter = "-oa";scale = 30;break;
            case 3: pattern = Pattern.compile("^.*[o][a]([0-9]{2,4}).*$");
                parts = first.split("-oa[0-9]{2,4}");splitter = "-oa";scale = 30;break;
            case 5: pattern = Pattern.compile("^.*[o][r]([0-9]{2,5}).*$");
                parts = first.split("-or[0-9]{2,5}");splitter = "-or";scale = 10;break;
			default: maxValue = 0;sizeOfPages = 0;break;
		}
		//tutaj program wyszukuje ostatnia stronę
	    for (int p=0;p<sizeOfPages;p++){
        	Matcher matcher = pattern.matcher(pagesList.get(p));
        	if (matcher.find()){
				tmpValue = Integer.parseInt(matcher.group(1));
				if(tmpValue > maxValue ){
					maxValue = tmpValue;
				}
		    }	
		}
		
		System.out.printf("Stage %d, maxValue %d \n",stage,maxValue);
		//tutaj tworzone sa adresy na podstawie ostatniej strony
		while(maxValue>0){
			nextUrl.add(parts[0]+ splitter +String.valueOf(maxValue)+parts[1]);
			maxValue -= scale;
		}
		switch (inception) {
            case 1:  pagesCategories.addAll(nextUrl);break;
			case 3:  pagesClasses.addAll(nextUrl);break;
			case 5:  pagesReviews.addAll(nextUrl);break;
			default: System.out.println("Inception");break;
		} 
    }	
    //zapisywanie adresów do odpowiedniej tablicy
    public static void getReviews(ArrayList<String> urlsSet)throws Exception {
		Set<String> rawCategories = new HashSet<String>();
		Set<String> rawClasses = new HashSet<String>();
		Set<String> rawReviews = new HashSet<String>();

		for(int j = 0; j < urlsSet.size(); j++) {
            if(urlsSet.get(j).contains("_Review") && !urlsSet.get(j).contains("-or") && stage <5 && !urlsSet.get(j).contains("Registration") ){
                mainReviews.add(urlsSet.get(j));
				
			}else if(urlsSet.get(j).contains("_Review") && urlsSet.get(j).contains("-or")){
                rawReviews.add(urlsSet.get(j));

            }else if(urlsSet.get(j).contains("-c") && !urlsSet.get(j).contains("-oa") && stage < 3){
				mainClasses.add(urlsSet.get(j));

			}else if(urlsSet.get(j).contains("-c") && urlsSet.get(j).contains("-oa")){
                rawClasses.add(urlsSet.get(j));

            }else if(urlsSet.get(j).contains("-oa") && !urlsSet.get(j).contains("-c") && stage <2){
				rawCategories.add(urlsSet.get(j));
			}
        }
		if(rawCategories.size()!=0 && stage == 1){
			addNextPages(rawCategories,stage);
		}
		if(rawClasses.size()!=0 && stage == 3){
            addNextPages(rawClasses,stage);
        }
		if(rawReviews.size()!=0 && stage == 5){
            addNextPages(rawReviews,stage);
        }
    }
	//metoda pobiera zawartość kodu HTML strony i wyszukuje linki oraz komentarze
    public static void getListOfLinks(ArrayList<String> stageArray,int i)throws Exception{
		ArrayList<String> urls = new ArrayList<String>();
		ArrayList<String> uniqueUrls = new ArrayList<String>();
		
		Document response = Jsoup.connect(stageArray.get(i)).get();
        Elements links = response.select("a[href]");        
		for (int m=0;m<links.size();m++){
			if(links.get(m).attr("abs:href").contains(".html")){
				urls.add(links.get(m).attr("abs:href").substring(0, links.get(m).attr("abs:href").indexOf(".html")+5));
			}
		}
		
		if(stage> 4 && stage <7){
			Elements wrap = response.select("div.innerBubble");
        	for(int x =0; x<wrap.size();x++)
                if(wrap.get(x).select("p.partial_entry").select("span").text().contains("Więcej")){
                    commentsLinks.add(wrap.get(x).select("a[href]").attr("abs:href"));
                }else{
                	fullComments.add(wrap.get(x).select("p.partial_entry").text()); 
             	}
		}else if(stage==7){
			Elements wrap = response.select("div.innerBubble");
			fullComments.add(wrap.get(0).select("p.partial_entry").text());
		}
		//filtrowanie unikalnych adresów
		Set<String> set = new HashSet<String>(urls);
		uniqueUrls.addAll(set);
        getReviews(uniqueUrls);
    }

    public static void main(String[] args)throws Exception {
		//nastepnę strony dla Hoteli się nie dodają, więc wykonałem obejście problemu
	    Set<String> workAround = new HashSet<String>();	
		workAround.add("https://pl.tripadvisor.com/Hotels-g274772-oa930-Krakow_Lesser_Poland_Province_Southern_Poland-Hotels.html");
		addNextPages(workAround,1);
		//dodanie kategorii
		mainCategories.add("https://pl.tripadvisor.com/Attractions-g274772-Activities-Krakow_Lesser_Poland_Province_Southern_Poland.html");
    	mainCategories.add("https://pl.tripadvisor.com/Hotels-g274772-Krakow_Lesser_Poland_Province_Southern_Poland-Hotels.html");
     	mainCategories.add("https://pl.tripadvisor.com/Restaurants-g274772-Krakow_Lesser_Poland_Province_Southern_Poland.html");
		//kolejne etapy programu
		stage = 1;
		for(int l = 0; l < mainCategories.size(); l++){
			System.out.println(""+mainCategories.get(l)+"\n");
			getListOfLinks(mainCategories,l);
		}
		//filtrowanie unikalnych kategorii - pod wiele miast
		Set<String> filterCategories = new HashSet<String>(pagesCategories);
        pagesCategories.clear();
        pagesCategories.addAll(filterCategories);

		stage = 2 ;
		for(int m = 0; m < pagesCategories.size(); m++){
			System.out.println("\n"+pagesCategories.get(m)+"\n");
            getListOfLinks(pagesCategories,m);
        }
		Set<String> filterClasses = new HashSet<String>(mainClasses);
		mainClasses.clear();
		mainClasses.addAll(filterClasses);

		stage = 3;
		for(int n = 0; n < mainClasses.size(); n++){
            System.out.println(""+mainClasses.get(n)+"\n");
            getListOfLinks(mainClasses,n);
		}
		Set<String> filterPagesClasses = new HashSet<String>(pagesClasses);
        pagesClasses.clear();
        pagesClasses.addAll(filterPagesClasses);

		stage = 4;
		for(int o = 0; o < pagesClasses.size(); o++){
            System.out.println(""+pagesClasses.get(o)+"\n");
            getListOfLinks(pagesClasses,o);
		}
		Set<String> filterReviews = new HashSet<String>(mainReviews);
        mainReviews.clear();
        mainReviews.addAll(filterReviews);

		stage = 5;
		for(int p = 0; p < mainReviews.size(); p++){
            System.out.println(""+mainReviews.get(p)+"\n");
            getListOfLinks(mainReviews,p);
		}
		Set<String> filterPagesReviews = new HashSet<String>(pagesReviews);
        pagesReviews.clear();
        pagesReviews.addAll(filterPagesReviews);

		stage = 6;
		for(int r = 0; r < pagesReviews.size(); r++){
            System.out.println(""+pagesReviews.get(r)+"\n");
            getListOfLinks(pagesReviews,r);
		}
		System.out.printf("commentsLinks1: %d \n",commentsLinks.size());
        Set<String> filterCommentsLinks = new HashSet<String>(commentsLinks);
        commentsLinks.clear();
        commentsLinks.addAll(filterCommentsLinks);

		stage = 7;	
		for(int s = 0; s < commentsLinks.size(); s++){
            System.out.println("\n"+commentsLinks.get(s)+"\n");
            getListOfLinks(commentsLinks,s);
		}
		//wywołanie zapisu do pliku 
		write("mainClasses.txt",mainClasses);
		write("mainReviews.txt",mainReviews);
		write("mainCategories.txt",mainCategories);
		write("pagesCategories.txt",pagesCategories);
		write("pagesClasses.txt",pagesClasses);
		write("pagesReviews.txt",pagesReviews);
		write("fullComments.txt",fullComments);
	}
}