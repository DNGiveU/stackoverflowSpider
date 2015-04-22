package spider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class ResolveThread extends ThreadInfo{
	   private String contentStr = "";
	/**
     * �ļ������߳�
     * @param hashCode hashֵ
     * @param path ·��
     * @param threadType �߳�����
     * @param context ������
     */
	public ResolveThread(long hashCode,String path,int threadType,Context context){
		super(hashCode, path, threadType, context);
	}

	@Override
	public void run() {
		resolve();
		 this.updateInfoStr();
		ThreadList.decreseRunningCount();
	}
	private void resolve(){
	       contentStr = openFile(path);
	       resolverStr();
	       
	    }
	 
    private String openFile( String path ){
       try {
           BufferedReader bis = new BufferedReader(new InputStreamReader(new FileInputStream( new File(path)),"utf-8" ) );
           String szContent="";
           String szTemp;
           
           while ( (szTemp = bis.readLine()) != null) {
               szContent+=szTemp+"\n";
           }
           bis.close();
           return szContent;
       }
       catch( Exception e ) {
    	   printThreadInfoError("�ļ���ʧ��",e);
    	   e.printStackTrace();
           return null;
       }
   }
    
    private void resolverStr(){
        try {
            Parser parser = new Parser(contentStr);
            NodeFilter filter = new NodeClassFilter(LinkTag.class);
            NodeList list = parser.extractAllNodesThatMatch(filter);
            switch(threadType){
            case ThreadInfo.ThreadType.SAVED_NEXT:
            	printThreadInfo("����");
	            for(int i = 0;i < list.size();i ++){
	                LinkTag node =(LinkTag)list.elementAt(i);
	                if(node.getLinkText().trim().equals("next")){
	                	System.out.println("TAG\tnext" + node.getLink());
	                	String new_Path = node.getLink();
	                	long hashcode = new_Path.hashCode();
	                	context.increaseNextIndex();
	                	context.getSaveList().addThread(new SaveThread(hashcode,new_Path,ThreadInfo.ThreadType.UNSAVE_NEXT,context));
	                }else if(node.getAttribute("class")!= null && node.getAttribute("class").equals("question-hyperlink")){
	                	System.out.println("TAG\tclass" + node.getLink());
	                	String new_Path = node.getLink();
	                	long hashcode = new_Path.hashCode();
	                	context.getSaveList().addThread(new SaveThread(hashcode,new_Path,ThreadInfo.ThreadType.UNSAVE,context));
	                }else{
	                	System.out.println("TAG\t�쳣" + node.getLink());
	                }
	            }
                break;
                
                case ThreadInfo.ThreadType.SAVED:
                	//TODO Html���ĵĽ���������Ϊpdf��ʽ�ļ�
                	
                	break;
            
            }
            this.updateInfoStr();
            printThreadInfo("");
        } catch (ParserException ex) {
        	printThreadInfoError("�ļ���ʧ��",ex);
            ex.printStackTrace();
        }
        
        
   }
}
