package spider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;



public class SaveThread extends ThreadInfo {
	private static final int HTTP_OK = 200;
    private static final int HTTP_BLOCK = 202;
    
    private CloseableHttpClient httpClient = null;
    
    /**
     * �ļ������߳�
     * @param hashCode hashֵ
     * @param path ·��
     * @param threadType �߳�����
	 * @param contentType ��������
     * @param context ������
     */
	public SaveThread(long hashCode,String path,int threadType,int contentType,Context context){
		super(hashCode, path, threadType,contentType, context);
	}


	/**
	 * ��ʽ���ļ����Ա��浽����
	 * @param fileName �ļ���
	 * @return ��ʽ������ļ���
	 */
	public static String formatFileName(String fileName){
//		�Ǻ� (*)
//		���� (|)
//		��б�� (\)
//		ð�� (:)
//		˫���� (��)
//		С�ں� (<)
//		���ں� (>)
//		�ʺ� (?)
//		��б�� (/)
		String[] replaceArray = new String[]{"\\","/",":","*","?","\"","<",">","|"};


		String temp = fileName.substring(fileName.lastIndexOf(File.pathSeparatorChar) + 1);

		for(int i = 0; i < replaceArray.length; i ++){
			fileName = fileName.replace(replaceArray[i], "_");
		}


		return fileName;
	}

	/**
	 * ��ʽ��·��
	 */
	public String formatPath(String path){


			Map<String,String> replaceMap = new HashMap<>();
			replaceMap.put("��","'");
			replaceMap.put("��","\"");
			replaceMap.put("\\\\","\\");
			replaceMap.put("�ٷֺ�","%");
			replaceMap.put("��","?");
			replaceMap.put("�»���","_");
			Iterator iterator = replaceMap.keySet().iterator();
			String key = null;

			while(iterator.hasNext()){
				key = (String)iterator.next();
				path = path.replace(key, replaceMap.get(key));
			}

			return path;
		}

	@Override
	public void run() {

		httpClient = (CloseableHttpClient) context.getHttpClient();

		HttpGet httpget = new HttpGet(formatPath(path));

		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(httpget);
			int code = response.getStatusLine().getStatusCode();
                        switch(code){
                        //TODO �ļ�·��Ҫ�޸ĵ�����
                            case HTTP_OK:
                                HttpEntity httpEntity = response.getEntity();
                                InputStream inputStream = httpEntity.getContent();
                                String filePath = Constant.applicationPath
                                        + "html" + File.separator
                                        + formatFileName(path.substring(path.lastIndexOf("/") + 1)) + ".html";

                                FileOutputStream fileOutputStream = null;
                                try{
                                    fileOutputStream = new FileOutputStream(filePath);
                                }catch(Exception e){
                                	printThreadInfoError("",e);
                                	e.printStackTrace();
                                }
                                int tempByte = -1;
                                while((tempByte = inputStream.read()) > 0){
                                    fileOutputStream.write(tempByte);
                                }
                                
                                path = filePath;
                                //�ر�������
                                if(inputStream != null){
                                    inputStream.close();
                                }
                                if(fileOutputStream != null){
                                    fileOutputStream.close();
                                }

								updateThreadCount();
								printThreadInfo("���н���");
                                new ResolveThread(hashCode, filePath, ++threadType,this.getContentType(),context);

								break;
                            case HTTP_BLOCK:
                            	printThreadInfo("��������ʧ��");

								this.updateThreadRunFailInfo();
                                break;
                        }
			
		} catch (Exception e) {

			this.updateThreadRunFailInfo();

			printThreadInfoError("����ʧ��",e);
			e.printStackTrace();
		} 
		

	}
}