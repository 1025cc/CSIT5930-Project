package com.csit5930.searchengine.crawler;



import java.io.*;
import java.util.*;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import java.util.ArrayList;


import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;


import org.htmlparser.beans.LinkBean;
import java.net.URL;

import java.sql.Date;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import com.csit5930.searchengine.model.WebPage;
import com.csit5930.searchengine.indexer.Indexer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class Crawler
{
    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);
    private static String url;
    public static Indexer indexer;

    Crawler() {
        url = null;
        indexer = new Indexer();
    }

    public void setURL(String _url) {
        url = _url;
    }

    public static Vector<String> extractTitle(String _url) throws ParserException
    {
        Vector<String> result = new Vector<String>();
        Parser parser = new Parser(_url);
        parser.setEncoding("UTF-8");
        NodeList list = new NodeList();
        NodeFilter filter = new TagNameFilter("title");
        for(NodeIterator e = parser.elements();e.hasMoreNodes();) {
            e.nextNode().collectInto(list,filter);
        }
        for(int i = 0; i < list.size(); i++) {
            Node e = list.elementAt(i);
            if(e instanceof TitleTag) {
                String str = ((TitleTag)e).getTitle();
                StringTokenizer st = new StringTokenizer(str);
                while (st.hasMoreTokens()) {
                    String a = st.nextToken();
                    if(a.matches("^[A-Za-z0-9]+")&&a!=null)
                        result.add(a);
                }
            }
        }
        return result;
    }

    public Vector<String> extractBody() throws ParserException
    {
        Vector<String> result = new Vector<String>();
        Parser parser = new Parser(url);
        parser.setEncoding("UTF-8");
        NodeList list = new NodeList();
        NodeFilter filter = new TagNameFilter("body");
        for(NodeIterator e = parser.elements();e.hasMoreNodes();) {
            e.nextNode().collectInto(list,filter);
        }
        for(int i = 0; i < list.size(); i++) {
            Node e = list.elementAt(i);
            if(e instanceof BodyTag) {
                String str = ((BodyTag)e).getBody();
                StringTokenizer st = new StringTokenizer(str);
                while (st.hasMoreTokens()) {
                    String a = st.nextToken();
                    if(a.matches("^[A-Za-z0-9]+")&&a!=null)
                        result.add(a);
                }
            }
        }
        return result;
    }
    public Vector<String> extractLinks() throws ParserException

    {
        // extract links in url and return them
        Vector<String> result = new Vector<String>();
        LinkBean bean = new LinkBean();
        bean.setURL(url);
        URL[] urls = bean.getLinks();
        for (URL s : urls) {
            result.add(s.toString());
        }
        return result;
    }
    public static Date getLastModifiedDate(String _url) throws IOException {
        URL u = new URL(_url);
        URLConnection connection = u.openConnection();
        Date date = new Date(connection.getLastModified());

        return date;
    }
    public static int getPageSize(String _url) throws IOException {
        URL u = new URL(_url);
        URLConnection connection = u.openConnection();
        BufferedReader b = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String input = "";
        String temp = "";
        while((input = b.readLine())!=null)
            temp += input;


        b.close();
        return temp.length();
    }
    public static void fetch()
    {
        try
        {
            Crawler crawler = new Crawler();

            int PageMax = 30;
            int pageID = 0;
            int n = 0;
            Map dict = new HashMap();

            ArrayList<String> visited_urls = new ArrayList<String>();


            Queue<String> queue = new LinkedList<String>();
            String initial_url = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
            queue.offer(initial_url);

            dict.put(initial_url,pageID);


            while (n < PageMax) {

                if(queue.isEmpty())
                {
                    break;
                }
                String url = queue.poll();
                crawler.setURL(url);
                // jsoup document
                Document doc = Jsoup.connect(url).get();

                // check url visited or not
                if (!visited_urls.contains(url)) {
                    visited_urls.add(url);
                }
                else{
                    while(visited_urls.contains(url)){
                        url = queue.poll();
                    }
                    visited_urls.add(url);
                }


                Object current_pageID = dict.get(url);
                logger.info(" ");
                logger.info("Current PageID: " + current_pageID);
                logger.info("Current URL: " + url);

                //extract text from title tag
                // Vector<String> words_title = crawler.extractTitle(url);

                //extract text from body tag
                // Vector<String> words = crawler.extractBody();

                String titles = doc.title();
                String texts  = doc.body().text();

                String cleaned_titles = titles.replaceAll("[\\pP\\p{Punct}]","");
                String cleaned_texts = texts.replaceAll("[\\pP\\p{Punct}]","");
                String dates =  getLastModifiedDate(url).toString();
                // String title = String.join(" ", words_title);
                // String body  = String.join(" ",words);


                logger.info("Title: " + cleaned_titles);
                logger.info("Body: " + cleaned_texts);
                logger.info("Page size: " + getPageSize(url));
                logger.info("Last Modified Date: " + getLastModifiedDate(url));
                System.out.println(" ");

                //String body, String title, String url, String lastModifiedDate, int pageSize
                WebPage webPage = new WebPage(cleaned_texts, cleaned_titles, url,dates, getPageSize(url));

                // check url in indexer or not
                Object InfoSphere = indexer.getPageInfoByUrl(url);
                if (InfoSphere!=null){
                    String old_date = indexer.getPageInfoByUrl(url).getLastModifiedDate();
                    if (!getLastModifiedDate(url).toString().contentEquals(old_date)){
                        // check last_modified_date != saved date
                        logger.info("Date changed -> indexer update");
                        indexer.updatePage(webPage);
                    }
                }
                else {
                    // index the page
                    n++;
                    indexer.indexPage(webPage);
                }




                // iterate the child urls by BFS

                Elements links = doc.select("a[href]");
                HashSet parent_url = new HashSet();
                parent_url.add(url);
                System.out.println("size: "+links.size());

                for(Element ele : links) {
                    try{
                        // check whether the url is valid
                        String link = ele.attr("abs:href");
                        logger.info(link);

                        if(link==null) {
                            continue;
                        }
                        else{
                            HashSet child_url = new HashSet();
                            String child = link;
                            child_url.add(child);

                            pageID++;
                            dict.put(child, pageID);
                            queue.offer(child);
                            // add parent and child urls
                            indexer.addChildLinks(url, child_url);
                            indexer.addParentLinks(child,parent_url);

                            }

                        }

                    catch(Exception e){}

                }

            }
        }

        catch (Exception e)
        {
            e.printStackTrace ();
        }

    }

    public static void main(String[] args){
        System.out.println("fetch started");
        Crawler.fetch();
        System.out.println("fetch finished");
//        System.out.println(indexer.getChildIdsByPageId(4));
//        System.out.println(indexer.getPageInfoById(2));
//        indexer.displayAllIndex();
//        System.out.println(indexer.getParentLinksByPageId(2));
//        System.out.println(indexer.getTfMax(2));


    }

}
