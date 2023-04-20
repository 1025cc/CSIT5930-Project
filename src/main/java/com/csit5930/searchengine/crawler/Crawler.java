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
import java.io.PrintWriter;
import java.io.File;


import org.htmlparser.beans.LinkBean;
import java.net.URL;

import java.sql.Date;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import com.csit5930.searchengine.model.WebPage;
import com.csit5930.searchengine.indexer.Indexer;


class Crawler
{
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

            int PageMax = 300;
            int pageID = 0;
            Map dict = new HashMap();

            ArrayList<String> visited_urls = new ArrayList<String>();

            File outputFile = new File("Spider.txt");
            PrintWriter writer = new PrintWriter(outputFile);

            Queue<String> queue = new LinkedList<String>();
            String initial_url = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
            queue.offer(initial_url);

            dict.put(initial_url,pageID);


            for(int n = 0; n < PageMax; n++) {

                if(queue.isEmpty())
                {
                    break;
                }
                String url = queue.poll();
                crawler.setURL(url);

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

                // check url in indexer or not
                while (indexer.getPageInfoByUrl(url)!=null) {
                    url = queue.poll();
                }

                Object current_pageID = dict.get(url);

                writer.println("");
                writer.println("");
                writer.println("Current PageID: " + current_pageID);
                writer.println("Current URL: "+url);

                System.out.println("Current PageID: " + current_pageID);
                System.out.println("Current URL: "+url);

                //extract text from title tag
                Vector<String> words_title = crawler.extractTitle(url);

                //extract text from body tag
                Vector<String> words = crawler.extractBody();

                //String body, String title, String url, String lastModifiedDate, int pageSize

                String dates =  getLastModifiedDate(url).toString();
                String title = String.join(" ", words_title);
                String body  = String.join(" ",words);

                writer.println("Title: "+words_title);
                writer.println("Body: "+words);
                writer.println("Page size: "+getPageSize(url));
                writer.println("Last Modified Date: "+getLastModifiedDate(url));
                writer.println("");

                System.out.println("Title: "+words_title);
                System.out.println("Body: "+words);
                System.out.println("Page size: "+getPageSize(url));
                System.out.println("Last Modified Date: "+getLastModifiedDate(url));
                System.out.println("");

                // index the page
                WebPage webPage = new WebPage(body,title,url,dates,getPageSize(url));
                indexer.indexPage(webPage);

                // iterate the child urls by BFS
                writer.println("Child URLs:");
                Vector<String> links = crawler.extractLinks();
                HashSet parent_url = new HashSet();
                parent_url.add(url);

                for(int i = 0; i < links.size(); i++) {
                    try{
                        URL url_fetch = new URL(links.get(i));
                        HttpURLConnection connection = (HttpURLConnection) url_fetch.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(2000);
                        connection.setReadTimeout(2000);

                        // check whether the url is valid
                        if (connection.getResponseCode() == 200){
                            writer.println(links.get(i));
                            System.out.println(links.get(i));

                            if(links.get(i)==null) {
                                continue;
                            }
                            else{
                                HashSet child_url = new HashSet();
                                String child = links.get(i);
                                child_url.add(child);
                                if(!queue.contains(child)) {
                                    pageID++;
                                    dict.put(child, pageID);
                                    queue.offer(child);
                                    // add parent and child urls
                                    indexer.addChildLinks(url, child_url);
                                    indexer.addParentLinks(child,parent_url);

                                }
                            }

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
        System.out.println(indexer.getTitlePostingListByWord("of"));
        System.out.println(indexer.getParentLinksByPageId(2));
        System.out.println(indexer.getTfMax(2));


    }

}