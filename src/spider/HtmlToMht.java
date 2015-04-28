package spider;

/**
 * Created by BruceDu on 2015/4/24.
 */

        import java.io.BufferedInputStream;
        import java.io.UnsupportedEncodingException;
        import java.io.ByteArrayInputStream;
        import java.io.File;
        import java.io.FileOutputStream;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.OutputStream;
        import java.net.MalformedURLException;
        import java.net.URLConnection;
        import java.net.URL;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.Iterator;
        import java.util.Map;
        import java.util.Properties;
        import java.util.StringTokenizer;

        import org.htmlparser.Parser;
        import org.htmlparser.Tag;
        import org.htmlparser.filters.TagNameFilter;
        import org.htmlparser.lexer.Lexer;
        import org.htmlparser.lexer.Page;
        import org.htmlparser.util.DefaultParserFeedback;
        import org.htmlparser.util.NodeList;
        import org.htmlparser.util.ParserException;
        import javax.activation.DataHandler;
        import javax.activation.DataSource;
        import javax.activation.MimetypesFileTypeMap;
        import javax.mail.Authenticator;
        import javax.mail.Message;
        import javax.mail.PasswordAuthentication;
        import javax.mail.Session;
        import javax.mail.internet.InternetAddress;
        import javax.mail.internet.MimeBodyPart;
        import javax.mail.internet.MimeMessage;
        import javax.mail.internet.MimeMultipart;

/**
 *
 * mht文件解析类
 *
 */
public class HtmlToMht {

    /** 网页编码 */
    private String strEncoding = null;

    // mht格式附加信息
    private String from = "lishigui@126.com";
    private String to = "lishigui@126.com";
    private String subject = "blog.csdn.net/lishigui";
    private String cc;
    private String bcc;

    public static void main(String[] args) {
        new HtmlToMht("http://www.baidu.com","C:");
    }

    /**
     * 构造方法：初始化<br>
     * 输入参数：strUrl 网页地址;  strFilePath 保存路径<br>
     */
    public HtmlToMht(String strUrl, String strFilePath) {

        try {
            byte[] bText = null;
            //取得页面内容
            bText = downBinaryFile(strUrl);
            String strText = new String(bText);
            strEncoding = strText.split("charset=", 2)[1];
            strEncoding = strEncoding.split("\"") [0];
                    System.err.println(strEncoding);
            try {
                strText = new String(bText, 0, bText.length, strEncoding);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (strText == null){
                return;
            }
            compile(new URL(strUrl),strText,strFilePath);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 方法说明：执行下载操作<br>
     * 输入参数：strWeb 网页地址; strText 网页内容; strFilePath 保存路径<br>
     * 返回类型：boolean<br>
     */
    public boolean compile(URL strWeb, String strText, String strFilePath) {
        if (strWeb == null || strText == null || strFilePath == null){
            return false;
        }
        HashMap urlMap = new HashMap();
        NodeList nodes = new NodeList();
        try {
            Parser parser = createParser(strText);
            nodes = parser.parse(null);
        } catch (ParserException e) {
            e.printStackTrace();
        }

        URL strWebB = extractAllScriptNodes(nodes);
        if(strWebB == null || strWebB.equals("")){
            strWebB = strWeb;
        }
        ArrayList urlScriptList = extractAllScriptNodes(nodes, urlMap, strWebB);
        ArrayList urlImageList = extractAllImageNodes(nodes, urlMap, strWebB);
        if(strWebB == null || strWebB.equals("")){
            for (Iterator iter = urlMap.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                String val = (String) entry.getValue();
                strText = strText.replace(val, key);
            }
        }

        try {
            createMhtArchive(strText, urlScriptList, urlImageList, strWeb, strFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    /**
     * 方法说明：下载文件操作<br>
     * 输入参数：url 文件路径<br>
     * 返回类型：byte[]<br>
     */
    public  byte[] downBinaryFile(String url){
        System.out.println(url);
        try {
            URL cUrl = new URL(url);
            URLConnection uc = cUrl.openConnection();
            uc.setConnectTimeout(3 * 1000);
            uc.connect();
            // String contentType = this.strType;
            int contentLength = uc.getContentLength();
            if (contentLength > 0) {
                InputStream raw = uc.getInputStream();
                InputStream in = new BufferedInputStream(raw);
                byte[] data = new byte[contentLength];
                int bytesRead = 0;
                int offset = 0;
                while (offset < contentLength) {
                    bytesRead = in.read(data, offset, data.length - offset);
                    if (bytesRead == -1) {
                        break;
                    }
                    offset += bytesRead;
                }
                in.close();
                raw.close();
                return data;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * 方法说明：建立HTML parser<br>
     * 输入参数：inputHTML 网页文本内容<br>
     * 返回类型：HTML parser<br>
     */
    private Parser createParser(String inputHTML) {
        Lexer mLexer = new Lexer(new Page(inputHTML));
        return new Parser(mLexer, new DefaultParserFeedback(
                DefaultParserFeedback.QUIET));
    }

    /**
     * 方法说明：抽取基础URL地址<br>
     * 输入参数：nodes 网页标签集合<br>
     * 返回类型：URL<br>
     */
    private URL extractAllScriptNodes(NodeList nodes) {

        NodeList filtered = nodes.extractAllNodesThatMatch(new TagNameFilter(
                "BASE"), true);

        if (filtered != null && filtered.size() > 0) {
            Tag tag = (Tag) filtered.elementAt(0);
            String href = tag.getAttribute("href");
            if (href != null && href.length() > 0) {
                try {
                    return new URL(href);
                } catch (MalformedURLException e) {
                    e.printStackTrace();

                }
            }
        }
        return null;
    }

    /**
     * 方法说明：抽取网页包含的css,js链接<br>
     * 输入参数：nodes 网页标签集合; urlMap 已存在的url集合<br>
     * 返回类型：css,js链接的集合<br>
     */
    private ArrayList extractAllScriptNodes(NodeList nodes, HashMap urlMap,
                                            URL strWeb) {

        ArrayList urlList = new ArrayList();
        NodeList filtered = nodes.extractAllNodesThatMatch(new TagNameFilter(
                "script"), true);
        //遍历页面所有的script结点
        for (int i = 0; i < filtered.size(); i++) {
            Tag tag = (Tag) filtered.elementAt(i);
            String src = tag.getAttribute("src");
            System.out.println("script src="+src);
            // Handle external css file's url
            if (src != null && src.length() > 0) {
                String innerURL = src;
                //取得绝对路径,即把?号后面的除掉
                String absoluteURL = makeAbsoluteURL(strWeb, innerURL);
                if (absoluteURL != null && !urlMap.containsKey(absoluteURL)) {
                    urlMap.put(absoluteURL, innerURL);
                    ArrayList urlInfo = new ArrayList();
                    urlInfo.add(innerURL);
                    urlInfo.add(absoluteURL);
                    urlList.add(urlInfo);
                }
                tag.setAttribute("src", absoluteURL);
            }
        }

        filtered = nodes.extractAllNodesThatMatch(new TagNameFilter("link"),true);
        for (int i = 0; i < filtered.size(); i++) {
            Tag tag = (Tag) filtered.elementAt(i);
            String type = tag.getAttribute("type");
            String rel = tag.getAttribute("rel");
            String href = tag.getAttribute("href");
            boolean isCssFile = false;
            if (rel != null) {
                isCssFile = rel.indexOf("stylesheet") != -1;
            } else if (type != null) {
                isCssFile |= type.indexOf("text/css") != -1;
            }

            if (isCssFile && href != null && href.length() > 0) {
                String innerURL = href;
                System.out.println("css link="+href);
                String absoluteURL = makeAbsoluteURL(strWeb, innerURL);
                if (absoluteURL != null && !urlMap.containsKey(absoluteURL)) {
                    urlMap.put(absoluteURL, innerURL);
                    ArrayList urlInfo = new ArrayList();
                    urlInfo.add(innerURL);
                    urlInfo.add(absoluteURL);
                    urlList.add(urlInfo);
                }
                tag.setAttribute("href", absoluteURL);
            }
        }

        return urlList;

    }

    /**
     * 方法说明：抽取网页包含的图像链接<br>
     * 输入参数：nodes 网页标签集合; urlMap 已存在的url集合; strWeb 网页地址<br>
     * 返回类型：图像链接集合<br>
     */
    private ArrayList extractAllImageNodes(NodeList nodes, HashMap urlMap,
                                           URL strWeb) {

        ArrayList urlList = new ArrayList();
        NodeList filtered = nodes.extractAllNodesThatMatch(new TagNameFilter(
                "IMG"), true);

        for (int i = 0; i < filtered.size(); i++) {
            Tag tag = (Tag) filtered.elementAt(i);
            String src = tag.getAttribute("src");
            System.out.println("IMG src="+src);
            // Handle external css file's url
            if (src != null && src.length() > 0) {
                String innerURL = src;
                String absoluteURL = makeAbsoluteURL(strWeb, innerURL);
                if (absoluteURL != null && !urlMap.containsKey(absoluteURL)) {
                    urlMap.put(absoluteURL, innerURL);
                    ArrayList urlInfo = new ArrayList();
                    urlInfo.add(innerURL);
                    urlInfo.add(absoluteURL);
                    urlList.add(urlInfo);
                }
                tag.setAttribute("src", absoluteURL);
            }
        }
        return urlList;
    }

    /**
     * 方法说明：相对路径转绝对路径<br>
     * 输入参数：strWeb 网页地址; innerURL 相对路径链接<br>
     * 返回类型：绝对路径链接<br>
     */
    public  String makeAbsoluteURL(URL strWeb, String innerURL) {

        // TODO Auto-generated method stub
        // 去除后缀(即参数去掉)
        int pos = innerURL.indexOf("?");
        if (pos != -1) {
            innerURL = innerURL.substring(0, pos);
        }
        if(strWeb == null || strWeb.equals("")){
            if(innerURL.startsWith("//")){
                innerURL = "http:"+innerURL;
            }
        }
        if (innerURL != null
                && innerURL.toLowerCase().indexOf("http") == 0) {
            return innerURL;
        }
        URL linkUri = null;
        try {
            linkUri = new URL(strWeb, innerURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;

        }

        String absURL = linkUri.toString();
        absURL = absURL.replace("../", "");
        absURL = absURL.replace("./", "");
        System.out.println(absURL);

        return absURL;

    }

    /**
     * 方法说明：创建mht文件<br>
     * 输入参数：content 网页文本内容; urlScriptList 脚本链接集合; urlImageList 图片链接集合
     * strWeb 网页地址； strFilePath 保存路径<br>
     * 返回类型：<br>
     */
    private void createMhtArchive(String content, ArrayList urlScriptList,
                                  ArrayList urlImageList, URL strWeb, String strFilePath) throws Exception {

        // Instantiate a Multipart object
        MimeMultipart mp = new MimeMultipart("related");

        Properties properties = new Properties();
        // 设置系统属性
        properties = System.getProperties();
        properties.put("mail.smtp.host", "smtp.126.com");
        properties.put("mail.smtp.auth", "true");
        // 邮件会话对象
        Session session = Session.getDefaultInstance(properties,
                new Email_auth(from, ""));

        // props.put("mail.smtp.host", smtp);
        MimeMessage msg = new MimeMessage(session);

        // set mailer
        msg.setHeader("X-Mailer", "Code Manager .SWT");

        // set from
        if (from != null) {
            msg.setFrom(new InternetAddress(from));
        }

        // set subject
        if (subject != null) {
            msg.setSubject(subject);
        }

        // to
        if (to != null) {
            InternetAddress[] toAddresses = getInetAddresses(to);
            msg.setRecipients(Message.RecipientType.TO, toAddresses);

        }

        // cc
        if (cc != null) {
            InternetAddress[] ccAddresses = getInetAddresses(cc);
            msg.setRecipients(Message.RecipientType.CC, ccAddresses);
        }

        // bcc
        if (bcc != null) {
            InternetAddress[] bccAddresses = getInetAddresses(bcc);
            msg.setRecipients(Message.RecipientType.BCC, bccAddresses);
        }

        // 设置网页正文
        MimeBodyPart bp = new MimeBodyPart();
        bp.setText(content, strEncoding);
        bp.addHeader("Content-Type", "text/html;charset=" + strEncoding);
        bp.addHeader("Content-Location", strWeb.toString());
        mp.addBodyPart(bp);

        int urlCount = urlScriptList.size();

        for (int i = 0; i < urlCount; i++) {

            bp = new MimeBodyPart();
            ArrayList urlInfo = (ArrayList) urlScriptList.get(i);
            String absoluteURL = urlInfo.get(1).toString();

            bp.addHeader("Content-Location",javax.mail.internet.MimeUtility
                    .encodeWord(java.net.URLDecoder.decode(absoluteURL, strEncoding)));

            DataSource source = new AttachmentDataSource(absoluteURL, "text");
            bp.setDataHandler(new DataHandler(source));

            mp.addBodyPart(bp);

        }

        urlCount = urlImageList.size();

        for (int i = 0; i < urlCount; i++) {

            bp = new MimeBodyPart();
            ArrayList urlInfo = (ArrayList) urlImageList.get(i);

            // String url = urlInfo.get(0).toString();
            String absoluteURL = urlInfo.get(1).toString();
            bp.addHeader("Content-Location",javax.mail.internet.MimeUtility
                    .encodeWord(java.net.URLDecoder.decode(absoluteURL, strEncoding)));

            DataSource source = new AttachmentDataSource(absoluteURL, "image");
            bp.setDataHandler(new DataHandler(source));

            mp.addBodyPart(bp);
        }
        msg.setContent(mp);
        // write the mime multi part message to a file
        msg.writeTo(new FileOutputStream(strFilePath+"//"+strWeb.toString().split("/")[strWeb.toString().split("/").length-1]+".mht"));
        // Transport.send(msg);

    }

    private InternetAddress[] getInetAddresses(String emails) throws Exception {
        ArrayList list = new ArrayList();
        StringTokenizer tok = new StringTokenizer(emails, ",");
        while (tok.hasMoreTokens()) {
            list.add(tok.nextToken());
        }
        int count = list.size();
        InternetAddress[] addresses = new InternetAddress[count];
        for (int i = 0; i < count; i++) {
            addresses[i] = new InternetAddress(list.get(i).toString());
        }
        return addresses;

    }

    class AttachmentDataSource implements DataSource {

        private MimetypesFileTypeMap map = new MimetypesFileTypeMap();
        private String strUrl;
        private String strType;
        private byte[] dataSize = null;

        /**
         *
         * This is some content type maps.
         */
        private Map normalMap = new HashMap();
        {
            // Initiate normal mime type map
            // Images
            normalMap.put("image", "image/jpeg");
            normalMap.put("text", "text/plain");

        }

        public AttachmentDataSource(String strUrl, String strType) {
            this.strType = strType;
            this.strUrl = strUrl;
            strUrl = strUrl.trim();
            strUrl = strUrl.replaceAll(" ", "%20");
            dataSize = downBinaryFile(strUrl);

        }

        public String getContentType() {
            return getMimeType(getName());
        }

        public String getName() {
            char separator = File.separatorChar;
            if (strUrl.lastIndexOf(separator) >= 0)
                return strUrl.substring(strUrl.lastIndexOf(separator) + 1);
            return strUrl;

        }

        private String getMimeType(String fileName) {
            String type = (String) normalMap.get(strType);
            if (type == null) {
                try {
                    type = map.getContentType(fileName);
                } catch (Exception e) {
                }
                if (type == null) {
                    type = "application/octet-stream";
                }
            }
            return type;

        }

        public InputStream getInputStream() throws IOException {
            if (dataSize == null)
                dataSize = new byte[0];
            return new ByteArrayInputStream(dataSize);
        }

        public OutputStream getOutputStream() throws IOException {
            return new java.io.ByteArrayOutputStream();
        }

    }

    class Email_auth extends Authenticator {

        String auth_user;
        String auth_password;

        public Email_auth() {
            super();
        }

        public Email_auth(String user, String password) {
            super();
            setUsername(user);
            setUserpass(password);

        }

        public void setUsername(String username) {
            auth_user = username;
        }

        public void setUserpass(String userpass) {
            auth_password = userpass;
        }

        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(auth_user, auth_password);
        }

    }

}

