package email.client.handler.receive;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import com.sun.mail.imap.IMAPMessage;

import email.client.gui.receive.ReceivedEmailStore;

/**
 * 负责处理邮件接收
 * 
 * @author baikkp
 * 
 *         2014-2-25 下午10:43:52
 * 
 */
public class SimpleEmailReceiver {

    private Store store;
    private Folder folder;

    /**
     * 
     * @param host
     *            服务器地址,将在前面加上"pop3."或"imap."前缀来访问相应的服务器地址
     * @param receiverAddress
     *            接收者邮箱地址
     * @param password
     *            接收者邮箱密码
     * @throws Exception
     */
    public ReceivedEmailStore handlerRective(String host,
	    String receiverAddress, String password) throws Exception {
	// 准备连接服务器的会话信息
	Properties props = new Properties();
	props.setProperty("mail.store.protocol", protocol);
	props.setProperty("mail.imap.host", protocol + "." + host);
	if ("pop3".equalsIgnoreCase(protocol)) {
	    props.setProperty("mail.imap.port", "110");
	} else {
	    props.setProperty("mail.imap.port", "143");
	}

	// 创建Session实例对象
	Session session = Session.getInstance(props);

	// 创建协议的Store对象
	store = session.getStore(protocol.toLowerCase());

	// 连接邮件服务器
	store.connect(receiverAddress, password);

	// 获得收件箱
	folder = store.getFolder("INBOX");
	// 以读写模式打开收件箱
	folder.open(Folder.READ_WRITE);

	// 获得收件箱的邮件列表
	Message[] messages = folder.getMessages();

	// 打印不同状态的邮件数量
	System.out.println("收件箱中共" + messages.length + "封邮件!");
	System.out.println("收件箱中共" + folder.getUnreadMessageCount() + "封未读邮件!");
	System.out.println("收件箱中共" + folder.getNewMessageCount() + "封新邮件!");
	System.out.println("收件箱中共" + folder.getDeletedMessageCount()
		+ "封已删除邮件!");

	List<Message> messageList = new ArrayList<>(messages.length);
	for(int i = 0; i < messages.length; i ++) {
	    messageList.add(messages[i]);
	}
	/*
	 * 黑名单发件人处理
	 */
	System.out.println("正在进行黑名单邮件过滤...");
	AntispamModule.blackListFilter(messageList);
	System.out.println("黑名单邮件过滤完成.");
	
	/*
	 * 反垃圾处理
	 */
	System.out.println("正在进行垃圾邮件过滤...");
	AntispamModule.antispamFilter(messageList);
	System.out.println("垃圾邮件过滤完成.");
	
	/**
	 * 需要将messages封装到ReceivedEmailStore实例中，再返回
	 */
	messages = new Message[messageList.size()];
	for(int i = 0; i < messageList.size(); i ++) {
	    messages[i] = messageList.get(i);
	}
	ReceivedEmailStore receivedEmailStore = ReceivedEmailStore.getInstance();
	receivedEmailStore.setFolder(folder);
	receivedEmailStore.setMessages(messages);
/*
	System.out
		.println("------------------------开始解析邮件----------------------------------");
	Scanner scan = new Scanner(System.in);
	for (int i = messages.length - 1; i >= 0; --i) {
	    IMAPMessage msg = (IMAPMessage) messages[i];
	    Class cls = msg.getClass();
	    System.out.printf("邮件%d的类型是%s,编码为%s\n", msg.getMessageNumber(),
		    cls.getName(), msg.getEncoding());
	    Method[] methods = cls.getMethods();
	    for (int j = 0; j < methods.length; ++j) {
		Method method = methods[j];
		if (method.getName().startsWith("get")
			&& method.getParameterTypes().length == 0) {
		    System.out.printf("%s() = ", method.getName());
		    scan.nextLine(); // / 等待确认

		    // 调用方法并输出结果
		    try {
			Object result = method.invoke(msg, new Object[] {});
			if (result != null
				&& result instanceof InternetAddress[]) {
			    InternetAddress[] addr = (InternetAddress[]) (result);
			    for (InternetAddress internetAddress : addr) {
				System.out.print(internetAddress.getAddress()
					+ ";");
			    }
			    System.out.println();
			} else {
			    System.out.println(result);
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    }
	    scan.nextLine();
	}
	System.out
		.println("------------------------解析完毕----------------------------------");
*/
	// 解析邮件
	/*
	 * for (int i = messages.length - 1; i >= 0; i--) { Message message =
	 * messages[i]; IMAPMessage msg = (IMAPMessage) message; String subject
	 * = MimeUtility.decodeText(msg.getSubject()); System.out.println("[" +
	 * subject + "]未读，是否需要阅读此邮件（yes/no）？"); BufferedReader reader = new
	 * BufferedReader(new InputStreamReader( System.in)); String answer =
	 * reader.readLine(); if ("yes".equalsIgnoreCase(answer)) {
	 * parseMessage(msg); // 解析邮件 // 第二个参数如果设置为true，则将修改反馈给服务器。false则不反馈给服务器
	 * msg.setFlag(Flag.SEEN, true); // 设置已读标志 } }
	 */

	return receivedEmailStore;
    }

    public void close() throws MessagingException {
	// 关闭资源
	folder.close(true);
	store.close();
    }

    private final String protocol;

    /**
     * 
     * @param protocol
     *            取值为"pop3",或"imap"
     */
    public SimpleEmailReceiver(String protocol) {
	this.protocol = protocol;
    }

    /**
     * 取得接收器使用的协议("pop3"或"imap")
     * 
     * @return
     */
    public String getProtocol() {
	return protocol;
    }

    /**
     * 解析邮件
     * 
     * @param messages
     *            要解析的邮件列表
     */
    public static void parseMessage(Message... messages)
	    throws MessagingException, IOException {
	if (messages == null || messages.length < 1)
	    throw new MessagingException("未找到要解析的邮件!");

	// 解析所有邮件
	for (int i = 0, count = messages.length; i < count; i++) {
	    MimeMessage msg = (MimeMessage) messages[i];
	    System.out.println("------------------解析第" + msg.getMessageNumber()
		    + "封邮件-------------------- ");
	    System.out.println("主题: " + getSubject(msg));
	    System.out.println("发件人: " + getFromName(msg) + "<"
		    + getFromEmail(msg) + ">");
	    System.out.println("收件人：" + getReceiveAddress(msg, null));
	    System.out.println("发送时间：" + getSentDate(msg, null));
	    System.out.println("是否已读：" + isSeen(msg));
	    System.out.println("邮件优先级：" + getPriority(msg));
	    System.out.println("是否需要回执：" + isReplySign(msg));
	    System.out.println("邮件大小：" + msg.getSize() / 1024 + "kb");
	    boolean isContainerAttachment = isContainAttachment(msg);
	    System.out.println("是否包含附件：" + isContainerAttachment);
	    if (isContainerAttachment) {
		saveAttachment(msg, "c:\\mailtmp\\" + msg.getSubject() + "_"); // 保存附件
	    }
	    StringBuffer content = new StringBuffer(30);
	    getMailTextContent(msg, content);
	    System.out.println("邮件正文："
		    + (content.length() > 100 ? content.substring(0, 100)
			    + "..." : content));
	    System.out.println("------------------第" + msg.getMessageNumber()
		    + "封邮件解析结束-------------------- ");
	    System.out.println();
	}
    }

    /**
     * 获得邮件主题
     * 
     * @param msg
     *            邮件内容
     * @return 解码后的邮件主题
     */
    public static String getSubject(MimeMessage msg)
	    throws UnsupportedEncodingException, MessagingException {
	return MimeUtility.decodeText(msg.getSubject());
    }

    /**
     * 获得发件人电邮地址
     * 
     * @param msg
     *            邮件内容
     * @return Email地址
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public static String getFromEmail(MimeMessage msg)
	    throws MessagingException {
	Address[] froms = msg.getFrom();
	if (froms == null || froms.length < 1)
	    throw new MessagingException("没有发件人!");

	InternetAddress address = (InternetAddress) froms[0];
	String from = address.getAddress();
	return from;
    }

    /**
     * 得到发件人名称
     * 
     * @param msg
     *            邮件内容
     * @return 发件人名称
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public static String getFromName(MimeMessage msg)
	    throws MessagingException, UnsupportedEncodingException {
	Address[] froms = msg.getFrom();
	if (froms == null || froms.length < 1)
	    throw new MessagingException("没有发件人!");

	InternetAddress address = (InternetAddress) froms[0];
	String fromName = address.getPersonal();
	if (fromName != null) {
	    fromName = MimeUtility.decodeText(fromName) + " ";
	} else {
	    fromName = "";
	}
	return fromName;
    }

    /**
     * 根据收件人类型，获取邮件收件人、抄送和密送地址。如果收件人类型为空，则获得所有的收件人
     * <p>
     * Message.RecipientType.TO 收件人
     * </p>
     * <p>
     * Message.RecipientType.CC 抄送
     * </p>
     * <p>
     * Message.RecipientType.BCC 密送
     * </p>
     * 
     * @param msg
     *            邮件内容
     * @param type
     *            收件人类型
     * @return 收件人1 <邮件地址1>, 收件人2 <邮件地址2>, ...
     * @throws MessagingException
     */
    public static String getReceiveAddress(MimeMessage msg,
	    Message.RecipientType type) throws MessagingException {
	StringBuffer receiveAddress = new StringBuffer();
	Address[] addresss = null;
	if (type == null) {
	    addresss = msg.getAllRecipients();
	} else {
	    addresss = msg.getRecipients(type);
	}

	if (addresss == null || addresss.length < 1)
	    throw new MessagingException("没有收件人!");
	for (Address address : addresss) {
	    InternetAddress internetAddress = (InternetAddress) address;
	    receiveAddress.append(internetAddress.toUnicodeString())
		    .append(",");
	}

	receiveAddress.deleteCharAt(receiveAddress.length() - 1); // 删除最后一个逗号

	return receiveAddress.toString();
    }

    /**
     * 获得邮件发送时间
     * 
     * @param msg
     *            邮件内容
     * @return yyyy年mm月dd日 星期X HH:mm
     * @throws MessagingException
     */
    public static String getSentDate(MimeMessage msg, String pattern)
	    throws MessagingException {
	Date receivedDate = msg.getSentDate();
	if (receivedDate == null)
	    return "";

	if (pattern == null || "".equals(pattern))
	    pattern = "yyyy年MM月dd日 E HH:mm ";

	return new SimpleDateFormat(pattern).format(receivedDate);
    }

    /**
     * 判断邮件中是否包含附件
     * 
     * @param msg
     *            邮件内容
     * @return 邮件中存在附件返回true，不存在返回false
     * @throws MessagingException
     * @throws IOException
     */
    public static boolean isContainAttachment(Part part)
	    throws MessagingException, IOException {
	boolean flag = false;
	if (part.isMimeType("multipart/*")) {
	    MimeMultipart multipart = (MimeMultipart) part.getContent();
	    int partCount = multipart.getCount();
	    for (int i = 0; i < partCount; i++) {
		BodyPart bodyPart = multipart.getBodyPart(i);
		String disp = bodyPart.getDisposition();
		if (disp != null
			&& (disp.equalsIgnoreCase(Part.ATTACHMENT) || disp
				.equalsIgnoreCase(Part.INLINE))) {
		    flag = true;
		} else if (bodyPart.isMimeType("multipart/*")) {
		    flag = isContainAttachment(bodyPart);
		} else {
		    String contentType = bodyPart.getContentType();
		    if (contentType.indexOf("application") != -1) {
			flag = true;
		    }

		    if (contentType.indexOf("name") != -1) {
			flag = true;
		    }
		}

		if (flag)
		    break;
	    }
	} else if (part.isMimeType("message/rfc822")) {
	    flag = isContainAttachment((Part) part.getContent());
	}
	return flag;
    }

    /**
     * 判断邮件是否已读
     * 
     * @param msg
     *            邮件内容
     * @return 如果邮件已读返回true,否则返回false
     * @throws MessagingException
     */
    public static boolean isSeen(MimeMessage msg) throws MessagingException {
	return msg.getFlags().contains(Flags.Flag.SEEN);
    }

    /**
     * 判断邮件是否需要阅读回执
     * 
     * @param msg
     *            邮件内容
     * @return 需要回执返回true,否则返回false
     * @throws MessagingException
     */
    public static boolean isReplySign(MimeMessage msg)
	    throws MessagingException {
	boolean replySign = false;
	String[] headers = msg.getHeader("Disposition-Notification-To");
	if (headers != null)
	    replySign = true;
	return replySign;
    }

    /**
     * 获得邮件的优先级
     * 
     * @param msg
     *            邮件内容
     * @return 1(High):紧急 3:普通(Normal) 5:低(Low)
     * @throws MessagingException
     */
    public static String getPriority(MimeMessage msg) throws MessagingException {
	String priority = "普通";
	String[] headers = msg.getHeader("X-Priority");
	if (headers != null) {
	    String headerPriority = headers[0];
	    if (headerPriority.indexOf("1") != -1
		    || headerPriority.indexOf("High") != -1)
		priority = "紧急";
	    else if (headerPriority.indexOf("5") != -1
		    || headerPriority.indexOf("Low") != -1)
		priority = "低";
	    else
		priority = "普通";
	}
	return priority;
    }

    /**
     * 获得邮件文本内容
     * 
     * @param part
     *            邮件体
     * @param content
     *            存储邮件文本内容的字符串
     * @throws MessagingException
     * @throws IOException
     */
    public static void getMailTextContent(Part part, StringBuffer content)
	    throws MessagingException, IOException {
	// 如果是文本类型的附件，通过getContent方法可以取到文本内容，但这不是我们需要的结果，所以在这里要做判断
	boolean isContainTextAttach = part.getContentType().indexOf("name") > 0;
	if (part.isMimeType("text/*") && !isContainTextAttach) {
	    content.append(part.getContent().toString());
	} else if (part.isMimeType("message/rfc822")) {
	    getMailTextContent((Part) part.getContent(), content);
	} else if (part.isMimeType("multipart/*")) {
	    Multipart multipart = (Multipart) part.getContent();
	    int partCount = multipart.getCount();
	    for (int i = 0; i < partCount; i++) {
		BodyPart bodyPart = multipart.getBodyPart(i);
		getMailTextContent(bodyPart, content);
	    }
	}
    }

    /**
     * 保存附件
     * 
     * @param part
     *            邮件中多个组合体中的其中一个组合体
     * @param destDir
     *            附件保存目录
     * @throws UnsupportedEncodingException
     * @throws MessagingException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void saveAttachment(Part part, String destDir)
	    throws UnsupportedEncodingException, MessagingException,
	    FileNotFoundException, IOException {
	if (part.isMimeType("multipart/*")) {
	    Multipart multipart = (Multipart) part.getContent(); // 复杂体邮件
	    // 复杂体邮件包含多个邮件体
	    int partCount = multipart.getCount();
	    for (int i = 0; i < partCount; i++) {
		// 获得复杂体邮件中其中一个邮件体
		BodyPart bodyPart = multipart.getBodyPart(i);
		// 某一个邮件体也有可能是由多个邮件体组成的复杂体
		String disp = bodyPart.getDisposition();
		if (disp != null
			&& (disp.equalsIgnoreCase(Part.ATTACHMENT) || disp
				.equalsIgnoreCase(Part.INLINE))) {
		    InputStream is = bodyPart.getInputStream();
		    saveFile(is, destDir, decodeText(bodyPart.getFileName()));
		} else if (bodyPart.isMimeType("multipart/*")) {
		    saveAttachment(bodyPart, destDir);
		} else {
		    String contentType = bodyPart.getContentType();
		    if (contentType.indexOf("name") != -1
			    || contentType.indexOf("application") != -1) {
			saveFile(bodyPart.getInputStream(), destDir,
				decodeText(bodyPart.getFileName()));
		    }
		}
	    }
	} else if (part.isMimeType("message/rfc822")) {
	    saveAttachment((Part) part.getContent(), destDir);
	}
    }

    /**
     * 读取输入流中的数据保存至指定目录
     * 
     * @param is
     *            输入流
     * @param fileName
     *            文件名
     * @param destDir
     *            文件存储目录
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void saveFile(InputStream is, String destDir, String fileName)
	    throws FileNotFoundException, IOException {
	BufferedInputStream bis = new BufferedInputStream(is);
	File savePath = new File(destDir);
	if (savePath.exists() == false) {
	    savePath.mkdirs();
	}
	BufferedOutputStream bos = new BufferedOutputStream(
		new FileOutputStream(new File(destDir + fileName)));
	int len = -1;
	while ((len = bis.read()) != -1) {
	    bos.write(len);
	    bos.flush();
	}
	bos.close();
	bis.close();
    }

    /**
     * 文本解码
     * 
     * @param encodeText
     *            解码MimeUtility.encodeText(String text)方法编码后的文本
     * @return 解码后的文本
     * @throws UnsupportedEncodingException
     */
    public static String decodeText(String encodeText) {
	if (encodeText == null || "".equals(encodeText)) {
	    return "";
	} else {
	    try {
		return MimeUtility.decodeText(encodeText);
	    } catch (UnsupportedEncodingException e) {
		e.printStackTrace();
		return "编码错误";
	    }
	}
    }

}
