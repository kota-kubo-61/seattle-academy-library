package jp.co.seattle.library.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jp.co.seattle.library.dto.BookDetailsInfo;
import jp.co.seattle.library.service.BooksService;

/**
 * Handles requests for the application home page.
 */
@Controller //APIの入り口
public class BulkBookController {
    final static Logger logger = LoggerFactory.getLogger(BulkBookController.class);

    @Autowired
    private BooksService booksService;


    @RequestMapping(value = "/bulkBook", method = RequestMethod.GET) //value＝actionで指定したパラメータ
    //RequestParamでname属性を取得
    public String login(Model model) {
        return "bulkBook";
    }

    /**
     * 書籍情報を登録する
     
     * @param file CSVファイル

     */
    @Transactional
    @RequestMapping(value = "/bulkinsertBook", method = RequestMethod.POST, produces = "text/plain;charset=utf-8")
    public String insertBook(Locale locale,
            @RequestParam("bulk_form") MultipartFile file,
            Model model) {
        logger.info("Welcome insertBooks.java! The client locale is {}.", locale);
        
        try {
            List<BookDetailsInfo> bookcsv = new ArrayList<BookDetailsInfo>();
            String line = null;
            InputStream stream = file.getInputStream();           
            Reader reader = new InputStreamReader(stream);
            BufferedReader buf= new BufferedReader(reader);
            int rowCount =1;
            boolean flag = false;
            String errorMessage = "";  
            while((line = buf.readLine()) != null) {
                String[]detail = line.split(",",6);          
                //必須項目が入っているか
                if(detail[0].isEmpty() || detail[1].isEmpty() || detail[2].isEmpty() 
                   || detail[3].isEmpty());{
                   errorMessage += rowCount + "行目で必要な情報がありません。";
                   flag = true;
                   }
                //出版日が正しく入力されているか
                   if(detail[3]!=null);
               try {
                  SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
                  df.setLenient(false);
                  df.parse(detail[3]);
              } catch (ParseException p2) {
                  errorMessage += rowCount + "行目は半角数字YYYYMMDD形式で入力してください。";
                  flag = true;
              } 
                //ISBNが正しく入力されているか
                if (detail[4] != null && !(detail[4].isEmpty())
                        && !(detail[4].matches("([0-9]{10}|[0-9]{13})"))) {
                    errorMessage += rowCount + "行目は半角数字10桁か13桁で入力してください。";
                    flag = true;
                }
                // パラメータで受け取った書籍情報をDtoに格納する。
                BookDetailsInfo bookInfo = new BookDetailsInfo();
                bookInfo.setTitle(detail[0]);
                bookInfo.setAuthor(detail[1]);
                bookInfo.setPublisher(detail[2]);
                bookInfo.setPublishDate(detail[3]);
                bookInfo.setIsbn(detail[4]);
                bookInfo.setDescription(detail[5]);

                bookcsv.add(bookInfo);
            }
            if (flag) {
                model.addAttribute("errorMessage", errorMessage);
                return "bulkBook";
            }

            // 書籍情報を新規登録する
            for (BookDetailsInfo book : bookcsv) {
                booksService.registBook(book);
            }

            model.addAttribute("resultMessage", "登録完了");

            // 登録した書籍の詳細情報を表示するように実装

            model.addAttribute("bookDetailsInfo", booksService.getBookInfo(booksService.getBookId()));

            //  詳細画面に遷移する
            return "bulkBook";

        } catch (IOException e) {
            model.addAttribute("errorimport", "CSVファイルの読み取りに失敗しました。");
            return "bulkBook";
        } catch (Exception ee) {
            model.addAttribute("errorimport", "CSVファイルの読み取りに失敗しました。");
            return "bulkBook";
        }


}

}
