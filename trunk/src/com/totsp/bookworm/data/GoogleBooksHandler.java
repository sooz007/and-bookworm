package com.totsp.bookworm.data;

import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * SAX DefaultHandler impl for Google Books feed.
 * 
 * TODO - compare with XMLPullParser impl - though direct SAX may still be faster?
 * http://www.developer.com/xml/article.php/3824221/Android-XML-Parser-Performance.htm 
 *
 * @author ccollins
 */
public class GoogleBooksHandler extends DefaultHandler {

   private static final String ENTRY = "entry";

   private final SimpleDateFormat dateFormat;
   private final ArrayList<Book> books;
   private Book book;

   private boolean inEntry;
   private int titleLevel = 0;
   StringBuilder sb;

   public GoogleBooksHandler() {
      this.books = new ArrayList<Book>();
      this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      this.sb = new StringBuilder();
   }

   @Override
   public void startDocument() throws SAXException {
   }

   @Override
   public void endDocument() throws SAXException {
   }

   @Override
   public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {

      if (localName.equals(GoogleBooksHandler.ENTRY)) {
         this.inEntry = true;
         this.book = new Book();
      }

      if (qName != null && qName.startsWith("dc:")) {

         if (this.inEntry && localName.equals("title")) {
            this.titleLevel++;
            this.sb.setLength(0);
         } else if (this.inEntry && localName.equals("date")) {
            this.sb.setLength(0);
         } else if (this.inEntry && localName.equals("creator")) {
            this.sb.setLength(0);
         } else if (this.inEntry && localName.equals("identifier")) {
            this.sb.setLength(0);
         } else if (this.inEntry && localName.equals("publisher")) {
            this.sb.setLength(0);
         } else if (this.inEntry && localName.equals("subject")) {
            this.sb.setLength(0);
         } else if (this.inEntry && localName.equals("description")) {
            this.sb.setLength(0);
         } else if (this.inEntry && localName.equals("format")) {
            this.sb.setLength(0);
         } else if (this.inEntry && localName.equals("link")) {
            this.sb.setLength(0);
            // parse/process the links (attributes), find gBooks page, find images, etc
            // use rel attribute = "http://schemas.google.com/books/2008/thumbnail" for image
            // use rel attribute = "http://schemas.google.com/books/2008/info" for overview web page
            // others are available, preview, etc, but not all books have such features (have to cross check with other feed items)
            // TODO only authenticated users can use this stuff?
            /*
            String rel = this.getAttributeValue("rel", atts);
            if (rel.equalsIgnoreCase("http://schemas.google.com/books/2008/thumbnail")) {
               book.setImageUrl(this.getAttributeValue("href", atts));
            } else if (rel.equalsIgnoreCase("http://schemas.google.com/books/2008/info")) {
               book.setOverviewUrl(this.getAttributeValue("href", atts));
            } 
            */
         }
      }
   }

   @Override
   public void endElement(final String namespaceURI, final String localName, final String qName) throws SAXException {
      if (localName.equals(GoogleBooksHandler.ENTRY)) {
         if (this.inEntry) {
            this.books.add(this.book);
            this.inEntry = false;
         }
      }

      if (qName != null && qName.contains("dc:")) {
         
         String bufferContents = sb.toString().replaceAll("\\s+", " "); 
         
         if (this.inEntry && localName.equals("title")) {
            if (this.titleLevel == 1) {
               this.book.setTitle(bufferContents);
            } else if (this.titleLevel == 2) {
               this.book.setSubTitle(bufferContents);
            }
         } else if (this.inEntry && localName.equals("date")) {
            try {
               Date d = this.dateFormat.parse(bufferContents);
               this.book.setDatePubStamp(d.getTime());
            } catch (ParseException e) {
               e.printStackTrace();
            }
         } else if (this.inEntry && localName.equals("creator")) {
            this.book.getAuthors().add(new Author(bufferContents));
         } else if (this.inEntry && localName.equals("identifier")) {
            String existingId = this.book.getIsbn();
            if ((existingId == null) || (existingId.length() < 13)) {
               String id = bufferContents;
               if (id.startsWith("ISBN")) {
                  this.book.setIsbn(id.substring(5, id.length()).trim());
               }
            }
         } else if (this.inEntry && localName.equals("publisher")) {
            this.book.setPublisher(bufferContents);
         } else if (this.inEntry && localName.equals("subject")) {
            this.book.setSubject(bufferContents);
         } else if (this.inEntry && localName.equals("description")) {
            this.book.setDescription(bufferContents);
         } else if (this.inEntry && localName.equals("format")) {
            if (this.book.getFormat() != null) {
               this.book.setFormat(new String(this.book.getFormat() + " " + bufferContents).trim());
            } else {
               this.book.setFormat(bufferContents);
            }            
         } else if (this.inEntry && localName.equals("link")) {
         }
      }
   }

   @Override
   public void characters(final char ch[], final int start, final int length) {
      this.sb.append(new String(ch, start, length));
   }

   /*
   private String getAttributeValue(final String attName, final Attributes atts) {
      String result = null;
      for (int i = 0; i < atts.getLength(); i++) {
         String thisAtt = atts.getLocalName(i);
         if (attName.equals(thisAtt)) {
            result = atts.getValue(i);
            break;
         }
      }
      return result;
   }
   */

   public ArrayList<Book> getBooks() {
      return this.books;
   }
}