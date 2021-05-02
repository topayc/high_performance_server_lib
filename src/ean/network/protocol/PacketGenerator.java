package ean.network.protocol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//지정된 클래스의 패키지 및 클래스 정보를 구함
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class ClassFinder {
	
	private String className;
	public ClassFinder(String className){
		this.className = className;
	}
	public String getPakageString() throws Exception {
		System.out.println(" =>입력된 " + className +" 으로 클래스 정보를 구성합니다" );
		String curPath = getClass().getResource(".").getPath();
		curPath = curPath.substring(1).replace("/", "\\");

		boolean isFind = false;
		String searchPath = null;
		String[] classPaths = System.getProperty("java.class.path").split(";");
		
		for (String classPath : classPaths) {
			if (curPath.contains(classPath)) {
				isFind = true;
				searchPath = classPath;
				break;
			}
		}

		if (!isFind) {
			System.out.println(className + " 클래스를 찾지 못했습니다");
			throw new Exception(className + " 클래스를 찾을 수 없습니다.");
		}

		File dir = new File(searchPath);
		if (!dir.isDirectory())
			throw new Exception("경로가 잘못되었습니다");
		
		ArrayList<String> fileList = new ArrayList<String>();
		getFileList(dir, fileList);

		String targetPath = null;
		isFind = false;
		for (String str : fileList) {
			if (str.contains(className)) {
				targetPath = str;
				isFind = true;
				break;
			}
		}
		
		if (!isFind)
			throw new Exception(className +" 클래스를 찾을 수 없습니다.");
		
		System.out.println(" =>" +className + " 클래스를 찾았습니다. 이 클래스의 패키지를 구성합니다");
		System.out.println("  " + targetPath);
		String str1 = targetPath.substring(searchPath.length() + 1);
		str1 = str1.replace("\\", ".");
		str1 = str1.substring(0, str1.length() - 6);
		System.out.println(" =>입력된 " + className +" 으로 클래스 정보를 구성하였습니다." );
		return str1;

	}

	private void getFileList(File targetPath, ArrayList<String> arr) {
		if (targetPath.isDirectory()) {
			String[] fl = targetPath.list();
			File tmpFile = null;
			for (int i = 0; i < fl.length; i++) {
				tmpFile = new File(targetPath.getAbsolutePath() + "/" + fl[i]);
				if (tmpFile.isDirectory()) {
					getFileList(tmpFile, arr);
				} else {
					arr.add(targetPath.getPath() + File.separator + fl[i]);
				}
			}
		}
	}
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//프로토콜의 파라미터 각각을 의미하는 클래스 
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Parameter {
	public String name;                   //ini 파일에서 키가 된다 (변수이름)
	public String type;                     // ini 파일에서 키의 값이 된다 ( 변수타입)
	public int Lentgh;                     // 배열형태일 경우만 유효하다.
	
	public Parameter(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {return name;}
	public void setName(String name) {this.name = name;}
	public String getType() {return type;}
	public void setType(String type) {this.type = type;}
	public int getLentgh() {return Lentgh;}
	public void setLentgh(int lentgh) {Lentgh = lentgh;}	
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//각각의 프로토콜을 의미하는 클래스 
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class ProtocolStructure {
	
	public String protocolName;  // ini 파일의 섹션이름을 의미한다.
	public List<Parameter> parameters;

	public ProtocolStructure(String protocolName) {
		this.protocolName = protocolName;
		parameters = new ArrayList<Parameter>();
	}
	
	public void addParemeter(Parameter p){this.parameters.add(p);}
	public String getProtocolName() {return protocolName;}
	public void setProtocolName(String protocolName) {this.protocolName = protocolName;}
	public List<Parameter> getParameters() {return parameters;}
	public void setParameters(List<Parameter> parameters) {this.parameters = parameters;}
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//전체 프로토콜 목록을 가지고 있는 클래스 
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Protocols extends ArrayList<ProtocolStructure>{
	private static final long serialVersionUID = 1L;
	public int protocolVersion;
	private String protocoVersionString;
	
	public Protocols(int version){
		this.protocolVersion = version;
	}
	
	public int getProtocolVersion() {return protocolVersion;}
	public void setProtocolVersion(int protocolVersion) {this.protocolVersion = protocolVersion;}
	public String getProtocoVersionString() {return protocoVersionString;}
	public void setProtocoVersionString(String protocoVersionString) {this.protocoVersionString = protocoVersionString;}
	
	
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//변수 타입을 나타내는 enum
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
enum VarType{
	BYTE(1),CHAR(2),INT(3),LONG(4),FLOAT(5),DOUBLE(6),ARR(7),STRING(8),USERTYPE(9);
	
	private int value;
	VarType(int value){
		this.value = value;
	}
	
	public int intValue(){
		return value;
	}
	public static VarType valueOfEx(String value){
		String str = value.toLowerCase();
		if (str.equals("byte")){return BYTE;}
		if (str.equals("string")){return STRING;}
		if (str.equals("char")){return CHAR;}
		if (str.equals("int")){return INT;}
		if (str.equals("long")){return LONG;}
		if (str.equals("float")){return FLOAT;}
		if (str.equals("double")){return DOUBLE;}
		if (str.equals("[]")){return ARR;}
		return USERTYPE;
	}
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//실제 INI 파일로 부터 프로토콜 정보를 읽어들려서 패킷 관련 클래스를 생성하는 클래스 
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class PacketGenerator {

	private static final String SECTION_VERSION = "PT_VERSION";
	private static final String SECTION_VERSION_KEY = "version";

	private List<String> createFiles = new ArrayList<String>();
	private Protocols pts ;
	private String prefix;
	private String packageName;
	private String fileName;
	
	private ClassFinder classFinder;
	private Ini iniFile;
	
	private InputStream is;
	private String charset;

	public PacketGenerator( String fileName,String streamClassName, String prefix,String charset) {
		this.fileName = fileName;
		this.prefix = prefix;
		this.charset = charset;
		this.classFinder = new ClassFinder(streamClassName);
	}

	public Protocols getPts() {return pts;}
	public void setPts(Protocols pts) {this.pts = pts;}
	public String getPrefix() {return prefix;}
	public void setPrefix(String prefix) {this.prefix = prefix;}
	public String getPackageName() {return packageName;}
	public void setPackageName(String packageName) {this.packageName = packageName;}
	public String getFileName() {return fileName;}
	public void setFileName(String fileName) {this.fileName = fileName;}
	public Ini getIniFile() {return iniFile;}
	public void setIniFile(Ini iniFile) {this.iniFile = iniFile;}
	public InputStream getIs() {return is;}
	public void setIs(InputStream is) {this.is = is;	}
	public String getCharset() {return charset;}
	public void setCharset(String charset) {this.charset = charset;}

	public void init() throws InvalidFileFormatException, IOException {
		String tmpFileName = fileName;
		is = getClass().getResourceAsStream(tmpFileName);
		iniFile = new Ini(is);
	}

	
	private void make() throws Exception {
		System.out.println("=> ini 파일로 부터 프로토콜 구조체 정보를 읽어들이고 있습니다.");
		makeProtocolSectionName();
		
		System.out.println("=> 프로토콜 정보를 구조화 하고 있습니다. ");
		makeParamter();
		System.out.println("=> 프로토콜을 위한 모든 구조체 정보가 성공적으로 준비되었습니다.");
		
		System.out.println("=> 구조체 정보로 부터 프로토콜 전용 처리 클래스와 구조체 클래스를 생성하기 위한 초기화 작업을 진행중입니다..");
		System.out.println();
		
		makeProtocolClassFile();
		makeStructruedClassFile();
		makeClassFileForTransaction();
		
		reportResut();
	}


	private void makeProtocolSectionName() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(fileName)));
		String line = null;
		
		int version = Integer.parseInt(iniFile.get(SECTION_VERSION,SECTION_VERSION_KEY));
		//버젼 번호를 매개인자로 프로토콜 리스트 객체 생성 
		pts = new Protocols(version);
		
		try {
			while ((line = br.readLine()) != null) {
				if (!line.contains("[") && !line.contains("]") ) continue;
				
				int index1 = line.indexOf("[");
				int index2 = line.indexOf("]");
		
				String value = line.substring(index1+1,index2);
				if (checkNumber(value)) continue; 
				if (line.trim().equals("")) continue;
				
				String sectionName = line.substring(1,line.length() -1);
				ProtocolStructure  protocol = new ProtocolStructure(sectionName);
				pts.add(protocol);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//해당 문자열이 숫자인지 아닌지를 판단한다.
	public boolean checkNumber(String str){
		char check;
		if(str.equals(""))
		{
			//문자열이 공백인지 확인
			return false;
		}
		
		for(int i = 0; i<str.length(); i++){
			check = str.charAt(i);
			if( check < 48 || check > 58)
			{
				//해당 char값이 숫자가 아닐 경우
				return false;
			}		
		}		
		return true;
	}


	//파라메터를 생성함 
	private void makeParamter() {
		for (ProtocolStructure protocolStructure : pts){
			String protocolName = protocolStructure.protocolName;  // 프토토콜 이름은 ini 파일에서 섹션 이름을 의미 
			for (String key : iniFile.get(protocolName).keySet()){
				Parameter parameter = new Parameter(key, iniFile.get(protocolName).fetch(key));
				protocolStructure.addParemeter(parameter);
			}
		}
		//객체화된 프로토콜을 테스트로 출력해봄 
		System.out.println("=> 프로토콜 구조체 정보를 출력합니다.");
		System.out.println("");
		
		for (int i =0; i < pts.size(); i++){
			ProtocolStructure s = pts.get(i);
			System.out.println( s.getProtocolName());
			
			for ( Parameter p : s.getParameters()){	
				System.out.println( "   public "+ p.getType() + " " + p.getName()+ ";" );
			}
			System.out.println();
		}
	}

	// 프로토콜 상수 정의 클래스 생성 
	private void makeProtocolClassFile() throws FileNotFoundException, UnsupportedEncodingException {
		Class<?> cls = getClass();
		Package pack = cls.getPackage();
		String packageName = "";
		
		if (pack !=null){
			packageName = pack.getName();
		}
		String className = getPrefix() + getFileName().substring(0,getFileName().length() - 4)+"_DEFINITION";
		String dir =getClass().getResource(".").getPath().replace("bin", "src");
		String srcFileName = className + ".java";
		String preFix = "\tpublic static final int ";
		
		//생성된 파일명을 리스트에 추가 
		createFiles.add(srcFileName);
		
		int sequence = pts.getProtocolVersion();
		System.out.println("=> 프로토콜 정의 클래스 파일을 생성하고 있습니다." );
		System.out.println("  [class] ->  "+ className + " class");
		System.out.println("  [package] ->  " + packageName);
		System.out.println("  [dir] ->  "+dir);
		System.out.println("  [Source File] ->" +srcFileName );
		
		FileOutputStream fos = new FileOutputStream(dir +srcFileName);
		PrintStream ps = new PrintStream(fos,true,getCharset());
		if (packageName !=null && !packageName.trim().equals(""))
			ps.println("package " + packageName + ";");
		ps.println();
		
		ps.println("public class " + className + " { ");
		ps.println();
		
		for (ProtocolStructure s : pts){
			ps.println(preFix + s.getProtocolName()+ " = " + sequence++ + ";" );
		}
		
		ps.println();
		ps.println("}");
		ps.close();
	}

	//각각의 프로토콜에 대응하는 클래스 생성 
	private void makeStructruedClassFile() throws FileNotFoundException, UnsupportedEncodingException {
		Class<?> cls = getClass();
		Package pack = cls.getPackage();
		String packageName = "";
		
		if (pack !=null){
			packageName = pack.getName();
		}
		String dir =getClass().getResource(".").getPath().replace("bin", "src");
		String preFix = "\tpublic ";
		
		System.out.println("");
		System.out.println("=> 프로토콜 구조체[클래스] 파일을 생성하고 있습니다." );
		System.out.println("");
		
		for (int i = 0; i < pts.size(); i++){
			
			if (i == 0) continue;
			
			ProtocolStructure s = pts.get(i);
			String className = getPrefix() + s.getProtocolName();
			String srcFileName  = className + ".java";
			System.out.println("  [" + className + "] 을 생성합니다." );
			System.out.println("  [class] ->  "+ className + " class");
			System.out.println("  [package] ->  " + packageName);
			System.out.println("  [dir] ->  "+dir);
			System.out.println("  [Source File] ->" +srcFileName );
			System.out.println("");
			
			//생성된 파일명을 리스트에 추가 
			createFiles.add(srcFileName);
			
			FileOutputStream fos = new FileOutputStream(dir +srcFileName);
			PrintStream ps = new PrintStream(fos,true,getCharset());
			if (packageName !=null && !packageName.trim().equals(""))
				ps.println("package " + packageName + ";");
			ps.println();
			
			ps.println("public class " + className + " { ");
			ps.println();
			
			for (Parameter p : s.getParameters()){
				String type ;
				boolean isArray = false;
				if (p.getType().toLowerCase().contains("[")){
					type = p.getType().substring(0,p.getType().indexOf("[")+1) + "]";
					//length = p.getType().substring(p.getType().indexOf("["),p.getType().indexOf("]"));
					isArray = true;
				}else {
					type = p.getType();
				}
				
				ps.println(preFix + type + " " + p.getName()+ (isArray? " = new " + p.getType() + ";" : ";") );
			}
			
			ps.println();
			ps.println("}");
			ps.close();
		}		
	}
	
	//프로토콜 처리 클래스 파일 생성 
	private void makeClassFileForTransaction() throws Exception {
		String packageStreamClass = classFinder.getPakageString();
		Class<?> packClass = Class.forName(packageStreamClass);
		
		Class<?> cls = getClass();
		Package pack = cls.getPackage();
		String packageName = "";
		
		if (pack !=null){
			packageName = pack.getName();
		}
		
		String dir =getClass().getResource(".").getPath().replace("bin", "src");
		String preFix1 = "\tpublic void ";   // 버퍼의 내용을 가져올 때 생성되는 메서드의 프리픽스
		String preFix2 = "\tpublic int ";     // 버퍼에 데이타를 쓸 때 생성되는 메서드의 프리픽스
		String className = getPrefix() + "TRANSACTION_" + getFileName().substring(0, getFileName().length()-4);
		String srcFileName = className + ".java";
		
		//생성된 파일명을 리스트에 추가 
		createFiles.add(srcFileName);
		
		System.out.println("");
		System.out.println("=> PacketStream 를 이용한 프로토콜 처리 클래스 파일을 생성하고 있습니다." );
		System.out.println("");
		System.out.println("  [class] ->  "+ className + " class");
		System.out.println("  [package] ->  " + packageName);
		System.out.println("  [dir] ->  "+dir);
		System.out.println("  [Source File] ->" +srcFileName );
		
		FileOutputStream fos = new FileOutputStream(dir +srcFileName);
		PrintStream ps = new PrintStream(fos,true,getCharset());
		if (packageName !=null && !packageName.trim().equals(""))
			ps.println("package " + packageName + ";");
		ps.println();
		
		//import 구문 생성 
		ps.println("import java.nio.ByteBuffer;");
		if (packClass.getPackage() != null){
			ps.println("import " + packClass.getName()+";");
		}
		for (int i = 0; i <pts.size(); i++){
			if( i ==0) continue;
			ProtocolStructure s = pts.get(i);
			
			ps.println( (packageName.trim().equals("")? "" : "import " + packageName +"." + getPrefix() +  s.getProtocolName() + ";"));
		}
		
		ps.println();
		//클래스 본문 생성
		ps.println("public class " + className + " { ");
		ps.println();
		
		//멤버 생성 
		ps.println("\tpublic PacketStream stream;");
		ps.println();
		
		//생성자 생성
		ps.println("\tpublic "+ className + "(String encoding){");
		ps.println("\t\t stream = new PacketStream(encoding);");
		ps.println("\t}");
		ps.println();
		
		for (int i = 0; i <pts.size(); i++){
			if (i ==0) continue;
			ProtocolStructure s = pts.get(i);
			
			//READ 함수 작성 
			ps.println(preFix1 + "READ_"+ s.getProtocolName()+ "(ByteBuffer buffer, "+ getPrefix()+s.getProtocolName() +" parameter){");
			ps.println("\t\tstream.setBuffer(buffer);");
			for (int j =0; j<s.getParameters().size(); j++){
				Parameter p = s.getParameters().get(j);
				String type = p.getType();
				
				if (type.contains("[")){
					type = "[]";
				}
				VarType enumType = VarType.valueOfEx(type);
				switch(enumType){
				case BYTE:
					ps.println("\t\tparameter."+p.getName()+" = stream.readByte();");
					break;
				case CHAR:
					ps.println("\t\tparameter."+p.getName()+" = stream.readChar();");
					break;
				case INT:
					ps.println("\t\tparameter."+p.getName()+" = stream.readInt();");
					break;
				case LONG:
					ps.println("\t\tparameter."+p.getName()+" = stream.readLong();");
					break;
				case FLOAT:
					ps.println("\t\tparameter."+p.getName()+" = stream.readFloat();");
					break;
				case DOUBLE:
					ps.println("\t\tparameter."+p.getName()+" = stream.readDouble;()" );
					break;
				case ARR:
					int length =Integer.parseInt(p.getType().substring(p.getType().indexOf("[")+1, p.getType().indexOf("]")));
					makeArrReadMethod(ps,p.getName(),p.getName(),p.getType(), length);
					break;
				case STRING:
					ps.println("\t\tparameter."+p.getName()+" = stream.readString();" );
					break;
				case USERTYPE:
					makeUserTypeReadMethod(ps,p.getName(),p.getName(),p.getType());
					break;
				default:
	
					throw new Exception();
				}
			}
			
			ps.println("\t}");
			ps.println();
			
			//WRITE 함수 작성 
			ps.println(preFix2 + "WRITE_"+ s.getProtocolName()+ "(ByteBuffer buffer, "+ getPrefix()+s.getProtocolName() +" parameter){");
			ps.println("\t\tstream.setBuffer(buffer);");
			for (int j =0; j<s.getParameters().size(); j++){
				Parameter p = s.getParameters().get(j);
				String type = p.getType();
				
				if (type.contains("[")){
					type = "[]";
				}
				VarType enumType = VarType.valueOfEx(type);
				switch(enumType){
				case BYTE:
					ps.println("\t\tstream.writeByte(parameter."+p.getName() + ");" );
					break;
				case CHAR:
					ps.println("\t\tstream.writetChar(parameter."+p.getName() + ");" );
					break;
				case INT:
					ps.println("\t\tstream.writeInt(parameter."+p.getName() + ");" );
					break;
				case LONG:
					ps.println("\t\tstream.writeLong(parameter."+p.getName() + ");" );
					break;
				case FLOAT:
					ps.println("\t\tstream.writeFloat(parameter."+p.getName() + ");" );
					break;
				case DOUBLE:
					ps.println("\t\tstream.writeDouble(parameter."+p.getName() + ");" );
					break;
				case ARR:
					int length =Integer.parseInt(p.getType().substring(p.getType().indexOf("[")+1, p.getType().indexOf("]")));
					makeArrWriteMethod(ps,p.getName(),p.getName(),p.getType(), length);
					break;
				case STRING:
					ps.println("\t\tstream.writeString(parameter."+p.getName() + ");" );	
					break;
				case USERTYPE:
					makeUserTypeWriteMethod(ps,p.getName(),p.getName(),p.getType());
					break;
				default:
					throw new Exception();
				}
			}
			ps.println("\t\treturn stream.getLength();" );
			ps.println("\t}");
			ps.println();
		}
		ps.println("}");
		ps.close();
	}
	

	private void makeArrWriteMethod(PrintStream ps, String paramName, String name,
			String type,int length) throws Exception {
		if (isPrimitive(type)){
			makePrimitiveArrayWriteMethod(ps,paramName,name,type,length);
		}else {
			makeObjectArrayWriteMethod(ps,paramName, name,type,length);
		}
	}

	
	private void makeObjectArrayWriteMethod(PrintStream ps, String paramName,
			String name, String type,int length) throws Exception {
		int arrLength = length;
		type = type.substring(0, type.indexOf("["));
		ClassFinder finder = new ClassFinder(type);
		String fullName  = finder.getPakageString();
		Class<?> cls = Class.forName(fullName);
		
		Field[] fields = cls.getDeclaredFields();
		ps.println("\t\tfor (int j = 0; j <"+arrLength+"; j++){");
		for (int i =0; i < fields.length ; i++){
			Field field = fields[i];
			String fieldName = field.getName();
			String fieldType = field.getType().getSimpleName();
			if (!isArray(fieldType) && isPrimitive(fieldType)){
				ps.println("\t\t\tstream.write"+toFirstUpperCase(fieldType) + "(parameter."+paramName +"[j]."+ fieldName + ");" );
			}
			
			if (isArray(fieldType) && isPrimitive(fieldType)){
				int length1 = getDynamicArrLength(type, field);
				makePrimitiveArrayWriteMethod(ps, paramName+"[j]" +"." +fieldName ,name, fieldType,length1) ;
			}	
		}
		ps.println("\t\t}");
	}

	private void makePrimitiveArrayWriteMethod(PrintStream ps,String paramName, String name, String type,int length) {
		int arrLength = length;/*type.substring(type.indexOf("[")+1,type.indexOf("]"));*/
		
		if (type.contains("byte")){
			ps.println("\t\tstream.write"+toFirstUpperCase(removeArrMark(type)) + "(parameter."+paramName + "."+ name + ");" );	
			return;
		}
		
		ps.println("\t\t\tfor (int i = 0; i <"+arrLength+"; i++){");
		String tmp= "(parameter."+paramName + "[i]);";
		String tmp1 = tmp;
		if (paramName.contains(".")){
			tmp1 = "(parameter."+paramName+ "[i]);";
		}
		ps.println("\t\t\t\tstream.write"+toFirstUpperCase(removeArrMark(type)) + tmp1 );	
		ps.println("\t\t\t}");	
	
	
		
	}

	private void makeArrReadMethod(PrintStream ps, String paramName, String name,
			String type,int length) throws Exception {
		if (isPrimitive(type)){
			makePrimitiveArrayReadMethod(ps,paramName,name,type,length);
		}else {
			makeObjectArrayReadMethod(ps,paramName, name,type,length);	
		}
	}


	private void makeObjectArrayReadMethod(PrintStream ps, String paramName,String name, String type,int length) throws Exception {
		int arrLength = length;
		String type1 = type.substring(0, type.indexOf("["));
		ClassFinder finder = new ClassFinder(type1);
		String fullName  = finder.getPakageString();
		Class<?> cls = Class.forName(fullName);
		
		Field[] fields = cls.getDeclaredFields();
		ps.println("\t\tfor (int j = 0; j <"+arrLength+"; j++){");
		for (int i =0; i < fields.length ; i++){
			Field field = fields[i];
			String fieldName = field.getName();
			String fieldType = field.getType().getSimpleName();
			if (!isArray(fieldType) && isPrimitive(fieldType)){
				ps.println("\t\t\tparameter."+paramName+"[j]."+ fieldName+ " = stream.read" + toFirstUpperCase(fieldType)+ "();" );
			}
			
			if (isArray(fieldType) && isPrimitive(fieldType)){
				int length1 = getDynamicArrLength(type, field);
				makePrimitiveArrayWriteMethod(ps, paramName+"[j]" +"." +fieldName ,name, fieldType,length1) ;
			}	
		}
		ps.println("\t\t}");
	}

	private void makePrimitiveArrayReadMethod(PrintStream ps, String paramName,
			String name, String type, int length) {
		int arrLength = length;/*type.substring(type.indexOf("[")+1,type.indexOf("]"));	*/
		if (type.contains("byte")){
			ps.println("\t\tparameter."+paramName+" = stream.readBytes;(" + arrLength +" );" );
			return;
		}
		ps.println("\t\tfor (int i = 0; i <"+arrLength+"; i++){");
		ps.println("\t\t\tparameter."+paramName+"[i]"+ " = stream.read" + toFirstUpperCase(removeArrMark(type)+ "();" ));
		ps.println("\t\t}");	
	}

	private void makeUserTypeWriteMethod(PrintStream ps, String paramName,String name, String type) throws Exception {
		ClassFinder finder = new ClassFinder(type);
		String fullName = finder.getPakageString();
		Class<?> cls = Class.forName(fullName);
		
		Field[] fields = cls.getDeclaredFields();
		for (int i =0; i <fields.length; i++){
			Field field = fields[i];
			String fieldName = field.getName();
			String fieldType = field.getType().getSimpleName();	
			if (!isArray(fieldType) && isPrimitive(fieldType)){
				ps.println("\t\tstream.write"+toFirstUpperCase(fieldType) + "(parameter."+paramName + "."+ fieldName + ");" );	
			}
			
			if (isArray(fieldType) && isPrimitive(fieldType)){
				int length = getDynamicArrLength(type, field);
				makePrimitiveArrayWriteMethod(ps, name +"." +fieldName ,name, fieldType,length) ;
		   }
	   }
   }

	private void makeUserTypeReadMethod(PrintStream ps, String paramName,String name, String type) throws Exception {
		ClassFinder finder = new ClassFinder(type);
		String fullName = finder.getPakageString();
		Class<?> cls = Class.forName(fullName);
		
		Field[] fields = cls.getDeclaredFields();
		for (int i =0; i <fields.length; i++){
			Field field = fields[i];
			String fieldName = field.getName();
			String fieldType = field.getType().getSimpleName();	
			if (!isArray(fieldType) && isPrimitive(fieldType)){
				ps.println("\t\tparameter." + paramName+"." + fieldName+" = stream.read"+toFirstUpperCase(fieldType) +"();" );
			}
			
			if (isArray(fieldType) && isPrimitive(fieldType)){
				int length = getDynamicArrLength(type, field);
				makePrimitiveArrayReadMethod(ps, name +"." +fieldName ,name, fieldType,length) ;
			}
		}
	}
	
	private String removeArrMark(String data){
		return data.substring(0,data.indexOf("["));
	}
	
	private String toFirstUpperCase(String data){
		return data.substring(0,1).toUpperCase() + data.substring(1,data.length());
	}
	
	private boolean isArray(String data){
		if (data.contains("[")){
			return true;
		}
		return false;
	}
	
	private boolean isPrimitive(String data){
		if (isArray(data)){
			data = data.substring(0, data.indexOf("[")); 
		}
		data = data.toLowerCase();
		if (data.contains("byte") || data.contains("char") || data.contains("short")  || data.contains("int") || data.contains("long") 
				|| data.contains("float") || data.contains("double") || data.contains("string") ){
			return true;
		}
		return false;
	}
	
	private int getDynamicArrLength(String className, Field field){
		
		if (className.contains("[")){
			className = className.substring(0, className.indexOf("["));
		}
		ClassFinder finder = new ClassFinder(className);
		int length =0;
		try{
			Class<?> targetClass = Class.forName(finder.getPakageString());
			Object targetObject = targetClass.newInstance();
			
			String fieldName = field.getName();
			String typeName = field.getType().getSimpleName();;
			
			if (typeName.contains("[")){
				typeName = typeName.substring(0, typeName.indexOf("["));
				typeName = typeName.substring(0,1).toUpperCase() + typeName.substring(1);
			
				if (field.get(targetObject) instanceof byte[]){
					byte[] arr = (byte[])field.get(targetObject);
					length = arr.length;
				}
				
				else if ( field.get(targetObject) instanceof char[]){
					char[] arr = (char[])field.get(targetObject);
					length = arr.length;
				}
				
				else if ( field.get(targetObject) instanceof short[]){
					short[] arr = (short[])field.get(targetObject);
					length = arr.length;
				}
				
				else if ( field.get(targetObject) instanceof int[]){
					int[] arr = (int[])field.get(targetObject);
					length = arr.length;
				}
				
				else if ( field.get(targetObject) instanceof long[]){
					long[] arr = (long[])field.get(targetObject);
					length = arr.length;
				}
				
				else if ( field.get(targetObject) instanceof float[]){
					float[] arr = (float[])field.get(targetObject);
					length = arr.length;
				}
				
				else if ( field.get(targetObject)instanceof double[]){
					double[] arr = (double[])field.get(targetObject);
					length = arr.length;
				}
				
				else if ( field.get(targetObject) instanceof String[]){
					String[] arr = (String[])field.get(targetObject);
					length = arr.length;
				
				}else {
					Object[] arr = (Object[])field.get(targetObject);
					length = arr.length;
				}
			}
			}catch(Exception e){
			e.printStackTrace();
		}
			return length;
	}

	private void reportResut() {
		System.out.println( );
		System.out.println("=> 모든 작업이 성공적으로 완료되었습니다");
		System.out.println( );
		System.out.println("=> 생성된 파일 리스트" );
		
		int i = 0;
		for( String fileName : createFiles){
			String add = null;
			if (i ==0)
				add = " --------------[프로토콜 상수 정의 클래스 파일]";
			else if (i > 0 && i < 4)
				add = " --------------[프로토콜 별 클래스 파일]";
			else 
				add = " --------------[프로토콜을 처리를 담당하는  클래스 파일]";
					
			System.out.println("  ------>  " + fileName + add );
			i++;
		}
	}

	public static void main(String[] arg) throws Exception {
		PacketGenerator generator = null;
		String iniFileName = null;
		String streamClassName = null;
		String fileNamePrefix = null;
		String charset = null;
		
		if ( arg.length ==0){
			iniFileName = "TCP_PROTOCOL.ini";
		    streamClassName = "PacketStream";
		    fileNamePrefix = "C_";
		    charset = "utf-8";
		}else if (arg.length == 1){
			iniFileName = arg[0];
	        streamClassName = "PacketStream";
	        fileNamePrefix = "C_";
	        charset = "utf-8";
		}else if (arg.length == 2){
			 iniFileName = arg[0];
			 streamClassName = arg[1];
			 fileNamePrefix = "C_";
			 charset = "utf-8";
		}else if ( arg.length ==3){
			 iniFileName = arg[0];
			 streamClassName = arg[1];
			 fileNamePrefix = arg[2];
			 charset = "utf-8";
		}else if ( arg.length == 4){
			 iniFileName = arg[0];
			 streamClassName = arg[1];
			 fileNamePrefix = arg[2];
			 charset = arg[3];
		}else {
			System.out.println("USAGE : java PacketGenerator [iniFileName] [streamClassName] [filePrefix] [charset]");
			System.exit(0);
		}
		generator = new PacketGenerator(iniFileName,streamClassName,fileNamePrefix,charset);
		
		try {
			generator.init();
			generator.make();
		} catch (InvalidFileFormatException e) {e.printStackTrace();
		} catch (IOException e) {e.printStackTrace();}
		
	}

}
