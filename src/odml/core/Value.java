package odml.core;

/************************************************************************
 *	odML - open metadata Markup Language - 
 * Copyright (C) 2009, 2010 Jan Grewe, Jan Benda 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the  GNU Lesser General Public License (LGPL) as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * odML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.CRC32;
import javax.swing.tree.TreeNode;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.*;

/**
 * The {@link Value} class constitutes the further information of a property where only the value = content
 * is mandatory. It contains the following fields:
 * <ol>
 * <li>value - mandatory, its the value = content itself.</li>
 * <li>uncertainty - optional, an estimation of the value's uncertainty.</li>
 * <li>unit - optional, the vlaue's unit.</li>
 * <li>type - optional, the data type of the value.</li>
 * <li>filename - optional, the default file name which should be used when saving the object.</li>
 * <li>definition - optional, here additional comments on the value of the property can be given.</li>
 * <li>reference - optional, here additional comments on the value of the property can be given.</li>
 * <li>encoder - optional. If binary content is included in the {@link Value}, indicate the encoder used in the form.</li>
 * <li>checksum - optional. The checksum of the file included in the {@link Value}. State the checksum in the form algorithm$checksum (e.g. crc32$...).</li>
 * </ol> 
 *  Only the value = content is mandatory, the others are optional.
 *   
 * @since 06.2010
 * @author Jan Grewe, Christine Seitz
 *
 */
public class Value extends Object implements Serializable, Cloneable, TreeNode {

   static Logger                         logger           = LoggerFactory.getLogger(Value.class);
   private static final long             serialVersionUID = 147L;
   private String                        unit             = null, type = null, reference = null;
   private Object                        content, uncertainty;
   private String                        definition, filename, checksum, encoder;
   private Property                      associatedProperty;
   private final static SimpleDateFormat dateFormat       = new SimpleDateFormat("yyyy-MM-dd");
   private final static SimpleDateFormat datetimeFormat   = new SimpleDateFormat(
                                                                "yyyy-MM-dd hh:mm:ss");
   private final static SimpleDateFormat timeFormat       = new SimpleDateFormat("hh:mm:ss");
   private final static String           regExNTuple      = "(?i)[0-9]{1,};[0-9]{1,}";


   //*****************************************************************
   //**************				constructors			***********	
   //*****************************************************************
   /**
    * @param content
    * @param unit
    * @throws Exception
    */
   protected Value(Object content, String unit) throws Exception {
      this(content, unit, null, null);
   }


   /**
    * 
    * @param content
    * @param unit
    * @param uncertainty
    * @throws Exception
    */
   protected Value(Object content, String unit, Object uncertainty) throws Exception {
      this(content, unit, uncertainty, null);
   }


   /**
    * 
    * @param content
    * @param unit
    * @param uncertainty
    * @param type
    * @throws Exception
    */
   protected Value(Object content, String unit, Object uncertainty, String type) throws Exception {
      this(content, unit, uncertainty, type, null, null, null);
   }


   /**
    * Creates a Value from a Vector containing the value data in the following sequence:
    * "content","unit","uncertainty","type","fileName","definition","reference"
    * @param data {@link Vector} of Objects that contains the data in the sequence as the {@link Value}
    * @throws Exception 
    */
   protected Value(Vector<Object> data) throws Exception {
      this(data.get(0), (String) data.get(1), data.get(2), (String) data.get(3), (String) data
            .get(4), (String) data.get(5),
            (String) data.get(6));
   }


   /**
    * Constructor for a Value containing all possible information. Any of the arguments
    * may be null except for Object value and it's unit.
    * @param content
    * @param unit
    * @param uncertainty
    * @param type
    * @param filename
    * @param definition
    * @param reference
    * @throws Exception
    */
   protected Value(Object content, String unit, Object uncertainty, String type, String filename,
                   String definition, String reference) throws Exception {
      this(content, unit, uncertainty, type, filename, definition, reference, "", "");
   }


   protected Value(Object content, String unit, Object uncertainty, String type, String filename,
                   String definition, String reference, String encoder) throws Exception {
      this(content, unit, uncertainty, type, filename, definition, reference, encoder, "");
   }


   protected Value(Object content, String unit, Object uncertainty, String type, String filename,
                   String definition, String reference, String encoder, String checksum)
                                                                                        throws Exception {
      if (type == null || type.isEmpty()) {
         throw new Exception("Could not create Value! 'type' must not be null or empty!");
      }
      this.content = null;
      this.uncertainty = null;
      this.filename = new String();
      this.definition = new String();
      this.reference = new String();
      this.checksum = new String();
      this.type = type;
      if (content == null || content.toString().isEmpty()) {
         logger.warn("! value should not be empty except for terminologies!");
      }
      this.content = checkDatatype(content, type);
      if (this.content == null || this.content.toString().isEmpty()) {
         logger.warn("! value should not be empty except for terminologies!");
      }

      if (type.equalsIgnoreCase("binary")) {
         this.content = encodeContent(content);
      }
      //*** uncertainty
      if (uncertainty == null) {
         this.uncertainty = "";
      } else {
         try {
            this.uncertainty = uncertainty;
         } catch (Exception e) {
            this.uncertainty = "";
            logger.error("", e);
         }
      }
      //*** filename
      if (filename != null && !filename.isEmpty()) {
         this.filename = filename;
      }
      //*** definition  
      if (definition == null) {
         this.definition = "";
      } else {
         this.definition = definition;
      }
      //*** reference
      if (reference == null) {
         this.reference = "";
      } else {
         this.reference = reference;
      }
      //*** unit
      if (unit == null) {
         this.unit = "";
      } else {
         this.unit = unit;
      }
      //*** encoder
      if (encoder == null) {
         this.encoder = "";
      }
      //*** checksum
      if (checksum == null) {
         this.checksum = "";
      }
   }


   /**
    * Returns whether or not a {@link Value} is empty.
    * @return {@link Boolean}: true if value is empty, false otherwise.
    */
   public boolean isEmpty() {
      return (content == null)
            || (content != null && content instanceof String && ((String) content).isEmpty());
   }


   /**
    * Checks the passed values class and returns the odML type.
    * @param value {@link Object} the value;
    * @return {@link String} the type under which odml refers to it.
    */
   public static String inferType(Object value) {
      if (value instanceof String) {
         return "string";
      } else if (value instanceof Integer) {
         return "int";
      } else if (value instanceof Boolean) {
         return "boolean";
      } else if (value instanceof Date) {
         return "datetime";
      } else if (value instanceof Float) {
         return "float";
      } else if (value instanceof Double) {
         return "float";
      } else if (value instanceof URL) {
         return "url";
      } else if (value instanceof File) {
         return "binary";
      } else if (value instanceof Date) {
         return "date";
      } else {
         return "string";
      }
   }


   /**
    * Checks and converts the content passed to the Value.
    * @param content Object: The content that needs to be checked.
    * @param type String: The type of the content
    * @return returns the content in the correct class or null if an error occurred.
    */
   public static Object checkDatatype(Object content, String type) {
      if (content == null || content.toString().isEmpty()) {
         logger.info("Found empty content!!!");
         return null;
      }
      // check for int
      if (type.matches("(?i)int.*")) {
         if (content instanceof java.lang.Integer) {
            return content;
         }
         // integer could be masked as string
         else if (content instanceof java.lang.String) {
            if (((java.lang.String) content).contains(".") || ((String) content).contains(",")) {
               int index = ((String) content).indexOf(".");
               if (index == -1)
                  index = ((String) content).indexOf(",");
               content = ((String) content).substring(0, index);
            }
            return Integer.parseInt((String) content);
         } else if (content instanceof Number) {
            return ((Number) content).intValue();
         } else {
            logger.error("Cannot convert value of class " + content.getClass().getSimpleName()
                  + " to requested type: " + type);
            return null;
         }
      }
      // check for float
      else if (type.matches("(?i)float.*")) {
         if (content instanceof Number) {
            return ((Number) content).floatValue();
         } else if (content instanceof java.lang.String) { // float could be masked as string
            return Float.parseFloat((String) content);
         } else {
            logger.error("Cannot convert value of class " + content.getClass().getSimpleName()
                  + " to requested type " + type);
            return null;
         }
      }

      // check for string (string = oneWord in this case)!
      else if (type.matches("(?i)string") || type.matches("(?i)text")) {
         logger.debug("type specified:\tstring");
         if (content instanceof String) {
            return content;
         } else if (content instanceof Character) {
            return ((Character) content).toString();
         } else {
            logger.error("Error converting content of class: "
                  + content.getClass().getSimpleName() + " to requested type: " + type);
            return null;
         }
      }

      // check for n-tuple (format DIGITSxDIGITS)
      else if (type.matches("(?i)n-tuple")) {
         if (content instanceof String && ((String) content).matches(regExNTuple)) {
            return content;
         } else {
            logger.error("Value does not match the n-tuple definition (regExp: "
                  + regExNTuple + ")!");
            return null;
         }
      }

      // check for date (format yyyy-mm-dd)
      else if (type.matches("(?i)date")) {
         if (content instanceof java.util.Date) {
            try {
               Date date = dateFormat.parse(dateFormat.format(content));
               return date;
            } catch (Exception e) {
               logger.error(e.getMessage());
            }
         } else if (content instanceof java.lang.String) {
            try {
               Date date = dateFormat.parse((String) content);
               return date;
            } catch (Exception e) {
               logger.error("Cannot convert passed String : " + content
                     + " to a date value!");
               return null;
            }
         } else {
            logger.error("Cannot convert passed object of class: "
                  + content.getClass().getSimpleName()
                  + " to a date value!");
            return null;
         }
      }

      // check for time (format hh:mm:ss)
      else if (type.matches("(?i)time")) {
         if (content instanceof java.util.Date) {
            try {
               Date date = timeFormat.parse(timeFormat.format(content));
               return date;
            } catch (Exception e) {
               logger.error(e.getMessage());
            }
         } else if (content instanceof java.lang.String) {
            try {
               Date date = timeFormat.parse((String) content);
               return date;
            } catch (Exception e) {
               logger.error(e.getLocalizedMessage());
            }
         } else {
            logger.error("Cannot convert passed object of class: "
                  + content.getClass().getSimpleName()
                  + " to a time value!");
            return null;
         }
      }

      // check for datetime (format yyyy-MM-dd HH:mm:ss)
      else if (type.matches("(?i)datetime")) {
         if (content instanceof java.util.Date) {
            try {
               Date date = datetimeFormat.parse(datetimeFormat.format(content));
               return date;
            } catch (Exception e) {
               logger.error(e.getLocalizedMessage());
            }
         } else if (content instanceof java.lang.String) {
            try {
               Date date = datetimeFormat.parse((String) content);
               return date;
            } catch (Exception e) {
               logger.error(e.getLocalizedMessage());
            }
         } else {
            logger.error("Cannot convert passed object of class: "
                  + content.getClass().getSimpleName()
                  + " to a datetime value!");
            return null;
         }
      }

      // check for boolean
      else if (type.matches("(?i)bool.*")) {
         if (content instanceof java.lang.Boolean) {
            return content;
         } else if (content instanceof java.lang.String) {
            return Boolean.parseBoolean((String) content);
         } else {
            logger.error("Cannot convert object of class: "
                  + content.getClass().getSimpleName() + " to a " + type + ": value!");
            return null;
         }
      }

      // check for URL
      else if (type.matches("(?i)URL")) {
         if (content instanceof java.net.URL) {
            return content;
         } else if (content instanceof java.lang.String) {
            try {
               URL parsedUrl = new URL((String) content);
               return parsedUrl;
            } catch (MalformedURLException e) {
               logger.error(e.getLocalizedMessage());
            }
         } else {
            logger.error("Could not convert " + content.getClass().getSimpleName()
                  + " to required type: " + type);
            return null;
         }
      }
      // check for binary content
      else if (type.matches("(?i)binary")) {
         if (content instanceof java.lang.String || content instanceof File
               || content instanceof URL || content instanceof URI) {
            return content;
         } else {
            logger.error("Binary (String), File, URL, or URI content expected, "
                  + content.getClass().getSimpleName() + " found!");
            return null;
         }
      }

      // check for persons = string
      else if (type.matches("(?i)person")) {
         if (!(content instanceof java.lang.String)) {
            logger.error("Expect a person to be of class expected, not " + content.getClass());
            return null;
         } else {
            return content;
         }
      }

      // all cases checked > unknown type specified!
      else {
         type = "string";
         logger.warn("type unknown:\thandling as 'string':\tcorrect");
         return content;
      }
      return null;
   }


   /**
    * Checks one String Object more in detail, could be
    * string (oneWord): 		caseNumber 0
    * text (more words&lines): caseNumber 10
    * n-tuple (DIGIT;DIGIT): 	caseNumber 2
    * date (yyyy-mm-dd):		caseNumber 3
    * time (HH:mm:ss):			caseNumber 4
    * integer:					caseNumber 5
    * float:					caseNumber 55
    * boolean:					caseNumber 6
    * datetime (yyyy-mm-dd HH:mm:ss): caseNumber 7 (3+4 ;)
    * @param content {@link String}
    * @return {@link Integer}: the caseNumber for the String
    */
   protected static int checkStringsforDatatype(String content) {
      content = content.trim();
      int caseNumber = 0; // by default string = oneWord
      // case 10: theContent has whitespaces in it > 'text'

      // pattern matchers for strings

      // case 2: for n-tuple: format DIGITSxDIGITS
      //String regExNTuple = "(?i)[0-9]{1,};[0-9]{1,}";

      /* case 3: for date: 
       * max. 12 for months allowed
       * max. 31 for days allowed
       * ensuring that February has max 29 days (not checking for leap years...)
       */
      String regExDate = "[0-9]{4}-(((([0][13-9])|([1][0-2]))-(([0-2][0-9])|([3][01])))|(([0][2]-[0-2][0-9])))";
      String regExDateGeneral = "[0-9]{4}-[0-9]{2}-[0-9]{2}";

      /* case 4: for time:
       * max 24 hours
       * max 60 min
       * max 60 seconds
       */
      String regExTime = "(([01][0-9])|([2][0-4])):(([0-5][0-9])|([6][0])):(([0-5][0-9])|([6][0]))"; // for time
      String regExTimeGeneral = "[0-9]{2}:[0-9]{2}:[0-9]{2}";

      // case 5: for int:
      // possibility for signs +- followed by at least one digit. nothing else can be in the pattern (i.e.  !
      String regExInt = "^[+-]?[0-9]+$";
      // case 55: for float:
      // possibility for signs +- maybe followed by digits, then must have a '.', 
      // then must be followed by at least one digit. nothing else can be in the pattern!
      String regExFloat = "^[+-]?[0-9]*\\.[0-9]+$";

      // case 6: for bool:
      String regExBool = "(true)|(false)|1|0";

      // String regExURL = "";	// for URL

      //case 7: for datetime
      String regExDatetimeGeneral = "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}";

      if (content.matches(regExNTuple)) {
         caseNumber = 2;
         logger.debug("checkStringsForDatatype:\tfound 'n-tuplet'");
      } else if (content.matches(regExDateGeneral)) {
         if (content.matches(regExDate)) {
            caseNumber = 3;
            logger.debug("checkStringsForDatatype:\tfound 'date'");
         } else { // (6)66 eeevvvill! instead of first 6 > 3 for Date
            caseNumber = 366;
            logger.info("checkStringsForDatatype:\tfound 'date'-like thing: " + content);
         }
         if (content.matches(regExDatetimeGeneral)) {
            caseNumber = 7;
            logger.debug("checkStringsForDatatype:\tfound 'datetime'");
         }
      } else if (content.matches(regExTimeGeneral)) {
         if (content.matches(regExTime)) {
            caseNumber = 4;
            logger.debug("checkStringsForDatatype:\tfound 'time'");
         } else { // (6)66 eeevvvill! instead of first 6 > 4 for Time
            caseNumber = 466;
            logger.info("checkStringsForDatatype:\tfound 'time'-like thing: " + content);
         }
         if (content.matches(regExDatetimeGeneral)) {
            caseNumber = 7;
            logger.debug("checkStringsForDatatype:\tfound 'datetime'");
         }
      } else if (content.matches(regExInt)) {
         caseNumber = 5;
         logger.debug("checkStringsForDatatype:\tfound 'int'");
      } else if (content.matches(regExFloat)) {
         caseNumber = 55;
         logger.debug("checkStringsForDatatype:\tfound 'float'");
      } else if (content.matches(regExBool)) {
         caseNumber = 6;
         logger.debug("checkStringsForDatatype:\tfound 'bool'");
      } else if (content.matches(regExDatetimeGeneral)) {
         caseNumber = 7;
         logger.debug("checkStringsForDatatype:\tfound 'datetime'");
      } else if (content.contains(" ")) {
         caseNumber = 10;
         logger.debug("checkStringsForDatatype:\tfound 'text'");
      }
      return caseNumber;
   }


   //***************************************************************************************
   //*****					methods to handle binary content					**********
   //***************************************************************************************
   /**
    * Function to convert the content of the indicated file to an array of bytes.
    * Is primarily for internal use to Base64 encode binary data. 
    * @param file {@link File}: the file to convert.
    * @return byte[]: the array of bytes contained in the file.
    * @throws IOException
    */
   public static byte[] getBytesFromFile(File file) throws IOException {
      InputStream in = new FileInputStream(file);
      //Get the size of the file
      long length = file.length();
      //ensure that the file not larger than Integer.MAX_VALUE.
      if (length > Integer.MAX_VALUE) {
         throw new IOException("File exceeds max value: " + Integer.MAX_VALUE);
      }
      //Create the byte array to hold the data
      byte[] bytes = new byte[(int) length];
      //Read in the bytes
      int offset = 0;
      int numRead = 0;
      while (offset < bytes.length &&
            (numRead = in.read(bytes, offset, bytes.length - offset)) >= 0) {
         offset += numRead;
      }
      //Ensure all the bytes have been read
      if (offset < bytes.length) {
         throw new IOException("Could not completely read file" + file.getName());
      }
      //Close stream
      in.close();
      return bytes;
   }


   /**
    * Writes the value content which is Base64 encoded to disc.
    * @param content {@link String}: the value content.
    * @param outFile {@link File}: the File into which the decoded content should be written
    */
   public static void writeBinaryToDisc(String content, File outFile) throws Exception {
      if (outFile == null) {
         throw new Exception("Argument outFile not specified!");
      }
      //create the outputStream
      FileOutputStream os = null;
      try {
         os = new FileOutputStream(outFile);
      } catch (Exception e) {
         logger.error("", e);
         throw e;
      }
      //create the decoder
      Base64 base = new Base64();
      //decode the content
      byte[] bytes = base.decode(content);
      //write bytes to disc
      os.write(bytes);
      os.flush();
      os.close();
   }


   //****************************************************************
   //*****					getter & setter					**********
   //****************************************************************
   // associated property (so to say dad in tree)
   protected void setAssociatedProperty(Property property) {
      this.associatedProperty = property;
   }


   protected Property getAssociatedProperty() {
      return this.associatedProperty;
   }


   // value
   protected void setContent(Object content) {
      this.content = content;
   }


   protected Object getContent() {
      return this.content;
   }


   // unit
   protected void setUnit(String unit) {
      this.unit = unit;
   }


   protected String getUnit() {
      return this.unit;
   }


   // uncertainty
   protected void setUncertainty(Object uncertainty) {
      this.uncertainty = uncertainty;
   }


   protected Object getUncertainty() {
      return this.uncertainty;
   }


   // type
   protected void setType(String type) {
      this.type = type;
   }


   protected String getType() {
      return this.type;
   }


   // Filename
   protected void setFilename(String filename) {
      this.filename = filename;
   }


   protected String getFilename() {
      return this.filename;
   }


   // definition
   protected void setDefinition(String comment) {
      this.definition = comment;
   }


   protected String getDefinition() {
      return this.definition;
   }


   // reference
   protected void setReference(String reference) {
      this.reference = reference;
   }


   protected String getReference() {
      return this.reference;
   }


   // encoder
   protected void setEncoder(String encoder) {
      this.encoder = encoder;
   }


   protected String getEncoder() {
      return this.encoder;
   }


   // checksum
   protected void setChecksum(String checksum) {
      this.checksum = checksum;
   }


   protected String getChecksum() {
      return this.checksum;
   }


   public void compareToTerminology(Property termProp) {
      if (this.type != null && !this.type.isEmpty()) {
         if (!this.type.equalsIgnoreCase(termProp.getType())) {
            logger.warn("Value type (" + this.type
                  + ") does not match the one given in the terminology(" + termProp.getType()
                  + ")! To guarantee interoperability please ckeck. However, kept provided type.");
         }
      } else {
         try {
            checkDatatype(this.content, termProp.getType());
            this.setType(termProp.getType());
            logger.info("Added type information to value.");
         } catch (Exception e) {
            logger
                  .warn("Value is not compatible with the type information the terminology suggests ("
                        + termProp.getType() + "). Did nothing but please check");
         }
      }
      if (this.unit != null && !this.unit.isEmpty()) {
         if (!this.unit.equalsIgnoreCase(termProp.getUnit(0))) {
            logger.warn("Value unit (" + this.unit
                  + ") does not match the one given in the terminology(" + termProp.getUnit()
                  + ")! To guarantee interoperability please ckeck. However, kept provided unit.");
         }
      } else {
         if (termProp.getUnit() != null && !termProp.getUnit(0).isEmpty()) {
            this.setUnit(termProp.getUnit(0));
            logger.info("Added unit " + termProp.getUnit() + " information to value.");
         }
      }
   }


   /**
    * TODO
    * Compares the content of two values and returns whether they are equal. So far this
    * concerns only the value content. Not type,definition etc.
    * @param other
    * @return {@link Boolean} <b>true</b> if the content of two values matches, <b>false</b> otherwise.
    */
   public boolean isEqual(Value other) {
      if (this.content.toString() != other.content.toString()) {
         return false;
      }
      return true;
   }


   /**
    * Base64 encodes the content if it represents either a File, URL, URI, or String that can be converted to a file.
    * @param content
    * @return
    */
   private String encodeContent(Object content) {
      if (content == null) {
         return null;
      }
      logger.info("Encoding content: " + content.toString());
      String encoded = null;
      File file = null;
      if (content instanceof String) {
         try {
            URI uri = new URI((String) content);
            file = new File(uri);
         } catch (Exception e) {
            return (String) content;
         }
      } else if (content instanceof URL) {
         try {
            file = new File(((URL) content).toURI());
         } catch (Exception e) {
            logger.error("Could not create a file from the specified URL: " + content.toString());
            file = null;
         }
      } else if (content instanceof URI) {
         try {
            file = new File((URI) content);
         } catch (Exception e) {
            logger.error("Could not create a file from the specified URI: " + content.toString());
            file = null;
         }
      } else if (content instanceof File) {
         file = (File) content;
      } else {
         logger.error("Could not create a File from input! Class: "
               + content.getClass().getSimpleName() + " Content: " + content.toString());
         file = null;
      }
      if (file == null) {
         return "";
      }
      Base64 enc = new Base64();
      //the value has to be converted to String; if it is already just take it, if it is not
      //try different things 
      try {
         byte[] bytes = enc.encode(getBytesFromFile(file));
         CRC32 crc = new CRC32();
         crc.update(bytes);
         this.setChecksum("CRC32$" + crc.getValue());
         this.setFilename(file.getName());
         this.setEncoder("Base64");
         encoded = bytes.toString();
      } catch (Exception e) {
         logger.error("An error occurred during encoding: " + e.getLocalizedMessage());
      }
      return encoded;
   }


   //****************************************************************
   //*****					Overrides for TreeNode			**********
   //****************************************************************
   @Override
   public Enumeration<TreeNode> children() {
      return null;
   }


   @Override
   public boolean getAllowsChildren() {
      return false;
   }


   @Override
   public TreeNode getChildAt(int childIndex) {
      return null;
   }


   @Override
   public int getChildCount() {
      return 0;
   }


   @Override
   public int getIndex(TreeNode node) {
      return 0;
   }


   @Override
   public TreeNode getParent() {
      return this.getAssociatedProperty();
   }


   @Override
   public boolean isLeaf() {
      return true;
   }


   @Override
   public String toString() {
      String s = "";
      if (this.getContent() != null)
         s = s.concat(this.getContent().toString());
      if (this.getUncertainty() != null && !this.getUncertainty().toString().isEmpty())
         s = s.concat("+-" + this.getUncertainty().toString());
      if (this.getUnit() != null)
         s = s.concat(" " + this.getUnit());
      return s;
   }
}
