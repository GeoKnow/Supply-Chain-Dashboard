package org.aksw.behoerdenwegweiser;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = EnrichmentProperties.PREFIX)
public class EnrichmentProperties {

  public static final String PREFIX = "enrichment";

  @NotNull
  private String inputFile;
  @NotNull
  private String email;
  @NotNull
  private String outputFile;

  /**
   * @return the URI to read triples from (includes file: and plain file names)
   */
  public String getInputFile() {
    return this.inputFile;
  }

  public void setInputFile(String data) {
    this.inputFile = data;
  }

  /**
   * @return the email address to "sign" the Nominatim API requests
   */
  public String getEmail() {
    return this.email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * @return the file to write the enriched RDF data into
   */
  public String getOutputFile() {
    return this.outputFile;
  }

  public void setOutputFile(String outputFile) {
    this.outputFile = outputFile;
  }


}
