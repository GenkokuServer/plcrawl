@XmlSchema(
        namespace = "http://maven.apache.org/POM/4.0.0",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(
                        prefix = "xsi",
                        namespaceURI = "http://www.w3.org/2001/XMLSchema-instance"
                )
        }
)
package city.genkoku.plcrawl.maven;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;