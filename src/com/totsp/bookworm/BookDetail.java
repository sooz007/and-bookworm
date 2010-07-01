package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RatingBar.OnRatingBarChangeListener;

import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.DateUtil;
import com.totsp.bookworm.util.StringUtil;

import java.util.Date;


/**
 * Defines the details view screen for a single book.
 * Detail includes the cover image and all book data as well as providing
 * an interface to modify user metadata for the book (eg. ratings, etc).
 */
public class BookDetail extends Activity {

   private static final int MENU_EDIT = 0;
   private static final int MENU_WEB_GOOGLE = 1;
   private static final int MENU_WEB_AMAZON = 2;

   private BookWormApplication application;

   private ImageView bookCover;
   private TextView bookTitle;
   private TextView bookSubTitle;
   private TextView bookAuthors;
   private TextView bookSubject;
   private TextView bookDatePub;
   private TextView bookPublisher;
   private TextView bookDetailContent;

   private CheckBox ownStatus;
   private CheckBox lentStatus;
   private CheckBox readStatus;
   private RatingBar ratingBar;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.bookdetail);
      application = (BookWormApplication) getApplication();

      bookCover = (ImageView) findViewById(R.id.bookcover);
      bookTitle = (TextView) findViewById(R.id.booktitle);
      bookSubTitle = (TextView) findViewById(R.id.booksubtitle);
      bookAuthors = (TextView) findViewById(R.id.bookauthors);
      bookSubject = (TextView) findViewById(R.id.booksubject);
      bookDatePub = (TextView) findViewById(R.id.bookdatepub);
      bookPublisher = (TextView) findViewById(R.id.bookpublisher);
      bookDetailContent = (TextView) findViewById(R.id.bookdetailcontent);

      ownStatus = (CheckBox) findViewById(R.id.bookownstatus);
      lentStatus = (CheckBox) findViewById(R.id.booklentstatus);
      readStatus = (CheckBox) findViewById(R.id.bookreadstatus);
      ratingBar = (RatingBar) findViewById(R.id.bookrating);

      ownStatus.setOnCheckedChangeListener(new OnCheckedChangeListener() {
          public void onCheckedChanged(final CompoundButton button, final boolean isChecked) {
             saveOwnStatusEdit();
          }
       });

      lentStatus.setOnCheckedChangeListener(new OnCheckedChangeListener() {
          public void onCheckedChanged(final CompoundButton button, final boolean isChecked) {
             // TODO not sure why change listener fires when onCreate is init, but does
             saveLentStatusEdit();
          }
       });
      
      readStatus.setOnCheckedChangeListener(new OnCheckedChangeListener() {
         public void onCheckedChanged(final CompoundButton button, final boolean isChecked) {
            // TODO not sure why change listener fires when onCreate is init, but does
            saveReadStatusEdit();
         }
      });

      ratingBar.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
         public void onRatingChanged(final RatingBar rb, final float val, final boolean b) {
            saveRatingEdit();
         }
      });

      setViewData();
   }

   @Override
   public void onPause() {
      bookTitle = null;
      super.onPause();
   }

   // go back to Main on back from here
   @Override
   public boolean onKeyDown(final int keyCode, final KeyEvent event) {
      if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
         startActivity(new Intent(BookDetail.this, Main.class));
         return true;
      }
      return super.onKeyDown(keyCode, event);
   }

   private void saveRatingEdit() {
      Book book = application.selectedBook;
      if (book != null) {
         book.bookUserData.rating = (Math.round(ratingBar.getRating()));
         application.dataManager.updateBook(book);
      }
   }

   private void saveOwnStatusEdit() {
	      Book book = application.selectedBook;
	      if (book != null) {
	         book.bookUserData.own = (ownStatus.isChecked());
	         application.dataManager.updateBook(book);
	      }
	   }

   private void saveLentStatusEdit() {
	      Book book = application.selectedBook;
	      if (book != null) {
	         book.bookUserData.lent = (lentStatus.isChecked());
	         application.dataManager.updateBook(book);
	      }
	   }

   private void saveReadStatusEdit() {
      Book book = application.selectedBook;
      if (book != null) {
         book.bookUserData.read = (readStatus.isChecked());
         application.dataManager.updateBook(book);
      }
   }

   private void setViewData() {
      Book book = application.selectedBook;
      if (book != null) {
         if (application.debugEnabled) {
            Log.d(Constants.LOG_TAG, "BookDetail book present, will be displayed: " + book.toStringFull());
         }
         Bitmap coverImage = application.imageManager.retrieveBitmap(book.title, book.id, false);
         if (coverImage != null) {
            bookCover.setImageBitmap(coverImage);
         } else {
            bookCover.setImageResource(R.drawable.book_cover_missing);
         }

         bookTitle.setText(book.title);
         bookSubTitle.setText(book.subTitle);
         ratingBar.setRating(book.bookUserData.rating);
         ownStatus.setChecked(book.bookUserData.own);
         lentStatus.setChecked(book.bookUserData.lent);
         readStatus.setChecked(book.bookUserData.read);
         bookAuthors.setText(StringUtil.contractAuthors(book.authors));
         bookDetailContent.setText(book.title + "\n\n" +
        		                   book.description + "\n\n" + 
        		                   book.format + "\n\n" + 
        		                   book.publisher + ", " + DateUtil.format(new Date(book.datePubStamp))
        		                   );

         // we leave publication date publisher and subject out of landscape layout 
         // TODO could add these back if size specific layouts are added
         if (bookDatePub != null) {
             bookDatePub.setText(DateUtil.format(new Date(book.datePubStamp)));
         }
         
         if (bookSubject != null) {
             bookSubject.setText(book.subject);
          }

         if (bookPublisher != null) {
            bookPublisher.setText(book.publisher);
         }
      }
   }

   @Override
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      if (application.selectedBook == null) {
         Long id = savedInstanceState.getLong(Constants.BOOK_ID);
         if (id != null) {
            application.establishSelectedBook(id);
            if (application.selectedBook != null) {
               setViewData();
            } else {
               startActivity(new Intent(this, Main.class));
            }
         } else {
            startActivity(new Intent(this, Main.class));
         }
      }
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      if (application.selectedBook != null) {
         saveState.putLong(Constants.BOOK_ID, application.selectedBook.id);
      }
      super.onSaveInstanceState(saveState);
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      menu.add(0, BookDetail.MENU_EDIT, 0, getString(R.string.menuEdit)).setIcon(android.R.drawable.ic_menu_edit);
      menu.add(0, BookDetail.MENU_WEB_GOOGLE, 1, null).setIcon(R.drawable.google);
      menu.add(0, BookDetail.MENU_WEB_AMAZON, 2, null).setIcon(R.drawable.amazon);
      return super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      Uri uri = null;
      switch (item.getItemId()) {
         case MENU_EDIT:
            startActivity(new Intent(this, BookForm.class));
            return true;
         case MENU_WEB_GOOGLE:
            // TODO other Locales for GOOG URL?
            uri = Uri.parse("http://books.google.com/books?isbn=" + application.selectedBook.isbn10);
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
            return true;
         case MENU_WEB_AMAZON:
            uri =
                     Uri.parse("http://www.amazon.com/gp/search?keywords=" + application.selectedBook.isbn10
                              + "&index=books");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
            return true;
         default:
            return super.onOptionsItemSelected(item);
      }
   }
}