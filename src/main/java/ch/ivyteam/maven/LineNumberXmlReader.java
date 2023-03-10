package ch.ivyteam.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Xml reader that stores the line number where an element is located in the
 * given file
 */
public class LineNumberXmlReader {

  private final static String LINE_NUMBER_KEY_NAME = "lineNumber";

  public static Document readXml(final File xmlFile) throws IOException, SAXException {
    InputStream is = new FileInputStream(xmlFile);
    try {
      return readXml(is);
    } finally {
      is.close();
    }
  }

  public static int getLinePosition(Node node) {
    return (Integer) node.getUserData(LINE_NUMBER_KEY_NAME);
  }

  private static Document readXml(final InputStream is) throws IOException, SAXException {
    final Document doc;
    SAXParser parser;
    try {
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      parser = factory.newSAXParser();
      parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      doc = docBuilder.newDocument();
    } catch (final ParserConfigurationException e) {
      throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
    }
    final Stack<Element> elementStack = new Stack<Element>();
    final StringBuilder textBuffer = new StringBuilder();
    final DefaultHandler handler = new PositionalHandler(textBuffer, elementStack, doc);
    parser.parse(is, handler);
    return doc;
  }

  private static final class PositionalHandler extends DefaultHandler {

    private final StringBuilder textBuffer;
    private final Stack<Element> elementStack;
    private final Document doc;
    private Locator locator;

    private PositionalHandler(StringBuilder textBuffer, Stack<Element> elementStack, Document doc) {
      this.textBuffer = textBuffer;
      this.elementStack = elementStack;
      this.doc = doc;
    }

    @Override
    public void setDocumentLocator(final Locator locator) {
      this.locator = locator; // Save the locator, so that it can be used
                              // later for line tracking when traversing
                              // nodes.
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName,
            final Attributes attributes)
            throws SAXException {
      addTextIfNeeded();
      final Element el = doc.createElement(qName);
      for (int i = 0; i < attributes.getLength(); i++) {
        el.setAttribute(attributes.getQName(i), attributes.getValue(i));
      }
      el.setUserData(LINE_NUMBER_KEY_NAME, this.locator.getLineNumber(), null);
      elementStack.push(el);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) {
      addTextIfNeeded();
      final Element closedEl = elementStack.pop();
      if (elementStack.isEmpty()) { // Is this the root element?
        doc.appendChild(closedEl);
      } else {
        final Element parentEl = elementStack.peek();
        parentEl.appendChild(closedEl);
      }
    }

    @Override
    public void characters(final char ch[], final int start, final int length) throws SAXException {
      textBuffer.append(ch, start, length);
    }

    // Outputs text accumulated under the current node
    private void addTextIfNeeded() {
      if (textBuffer.length() > 0) {
        final Element el = elementStack.peek();
        final Node textNode = doc.createTextNode(textBuffer.toString());
        el.appendChild(textNode);
        textBuffer.delete(0, textBuffer.length());
      }
    }
  }
}
