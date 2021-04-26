package jp.co.seattle.library.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import jp.co.seattle.library.service.ThumbnailService;

/**
 * Handles requests for the application home page.
 */
@Controller //APIの入り口
public class EditBooksController {
    final static Logger logger = LoggerFactory.getLogger(EditBooksController.class);

    @Autowired
    private BooksService booksService;

    @Autowired
    private ThumbnailService thumbnailService;

    @RequestMapping(value = "/editBook", method = RequestMethod.POST) //value＝actionで指定したパラメータ
    //RequestParamでname属性を取得
    public String login(Model model,
            @RequestParam("bookId") Integer bookId) {
        model.addAttribute("bookDetailsInfo", booksService.getBookInfo(bookId));
        return "editBook";
    }

    /**
     * 書籍情報を編集する
     * @param locale ロケール情報
     * @param title 書籍名
     * @param description 書籍説明
     * @param author 著者名
     * @param publisher 出版社
     * @param publish_date 出版日
     * @param file サムネイルファイル
     * @param isbn isbn
     * @param model モデル
     * @return 遷移先画面
     */
    @Transactional
    @RequestMapping(value = "/reinsertBook", method = RequestMethod.POST, produces = "text/plain;charset=utf-8")
    public String reinsertBook(Locale locale,
            @RequestParam("bookId") int bookId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("author") String author,
            @RequestParam("publisher") String publisher,
            @RequestParam("publish_date") String publishDate,
            @RequestParam("thumbnail") MultipartFile file,
            @RequestParam("isbn") String isbn,
            Model model) {
        logger.info("Welcome insertBooks.java! The client locale is {}.", locale);

        // パラメータで受け取った書籍情報をDtoに格納する。
        BookDetailsInfo bookInfo = new BookDetailsInfo();
        bookInfo.setBookId(bookId);
        bookInfo.setTitle(title);
        bookInfo.setDescription(description);
        bookInfo.setAuthor(author);
        bookInfo.setPublisher(publisher);
        bookInfo.setPublishDate(publishDate);
        bookInfo.setIsbn(isbn);

        // クライアントのファイルシステムにある元のファイル名を設定する
        String thumbnail = file.getOriginalFilename();

        if (!file.isEmpty()) {
            try {
                // サムネイル画像をアップロード
                String fileName = thumbnailService.uploadThumbnail(thumbnail, file);
                // URLを取得
                String thumbnailUrl = thumbnailService.getURL(fileName);

                bookInfo.setThumbnailName(fileName);
                bookInfo.setThumbnailUrl(thumbnailUrl);

            } catch (Exception e) {

                // 異常終了時の処理
                logger.error("サムネイルアップロードでエラー発生", e);
                model.addAttribute("bookDetailsInfo", bookInfo);
                return "editBook";
            }
        }

        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
            df.setLenient(false);

            df.parse(publishDate);

        } catch (ParseException p) {

            model.addAttribute("errorLetter2", "出版日は半角数字のYYYYMMDD形式で入力してください。");

            return "editBook";
        }

        boolean isValidIsbn = isbn.matches("[0-9]{10}|[0-9]{13}");

        if (!isValidIsbn) {

            model.addAttribute("errorLetter3", "10桁または13桁の半角数字で入力してください。");

            return "editBook";

        }

        // 書籍情報を再度、登録し直す

        booksService.editBook(bookInfo);

        model.addAttribute("resultMessage", "登録完了");

        // TODO 登録した書籍の詳細情報を表示するように実装

        model.addAttribute("bookDetailsInfo", booksService.getBookInfo(booksService.getBookId()));

        //  詳細画面に遷移する
        return "details";
    }

}
