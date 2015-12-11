package org.aksw.behoerdenwegweiser;

import java.io.FileOutputStream;
import java.util.List;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.datatypes.BaseDatatype;

import fr.dudie.nominatim.client.JsonNominatimClient;
import fr.dudie.nominatim.client.NominatimClient;
import fr.dudie.nominatim.client.NominatimOptions;
import fr.dudie.nominatim.client.request.ExtendedSearchQuery;
import fr.dudie.nominatim.client.request.NominatimSearchRequest;
import fr.dudie.nominatim.client.request.SimpleSearchQuery;
import fr.dudie.nominatim.client.request.paramhelper.PolygonFormat;
import fr.dudie.nominatim.model.Address;
import fr.dudie.nominatim.model.PolygonPoint;

@SpringBootApplication
public class Application implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(Application.class);

  private static final Property GEO_LONG = ResourceFactory
      .createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#long");
  private static final Property GEO_LAT = ResourceFactory
      .createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
  private static final Property OGC_AS_WKT = ResourceFactory
      .createProperty("http://www.opengis.net/ont/geosparql#asWKT");
  //private static final Property OGC_WKT_LITERAL = ResourceFactory
  //    .createProperty("http://www.opengis.net/ont/geosparql#wktLiteral");
  private static final BaseDatatype OGC_WKT_LITERAL = new BaseDatatype("http://www.opengis.net/ont/geosparql#wktLiteral");

  @Autowired
  private EnrichmentProperties properties;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    Model inputModel = RDFDataMgr.loadModel(this.properties.getInputFile());
    // needed because we can't write into the input model while querying
    Model coordinatesModel = ModelFactory.createDefaultModel();
    NominatimOptions options = new NominatimOptions();
    options.setPolygonFormat(PolygonFormat.TEXT);
    NominatimClient nominatimClient =
        new JsonNominatimClient("http://nominatim.openstreetmap.org/", new DefaultHttpClient(), this.properties.getEmail(), options);

    // get the site addresses
    Query siteAddressQuery = QueryFactory.read("siteAddress.rq");
    QueryExecution siteAddressQueryExecution =
        QueryExecutionFactory.create(siteAddressQuery, inputModel);
    ResultSet siteAddressResultSet = siteAddressQueryExecution.execSelect();

    // iterate over sites
    while (siteAddressResultSet.hasNext()) {

      QuerySolution siteAddressBinding = siteAddressResultSet.next();

      String siteUri = siteAddressBinding.getResource("siteUri").toString();
      String streetAddress = siteAddressBinding.getLiteral("streetAddress").getString();
      String postalCode = siteAddressBinding.getLiteral("postalCode").getString();
      String city = siteAddressBinding.getLiteral("city").getString();
      // String city = siteAddressBinding.getLiteral("city").getString();

      // create Nominatim API request to get coordinates
      ExtendedSearchQuery query = new ExtendedSearchQuery();
      query.setStreet(streetAddress);
      query.setPostalCode(postalCode);
      query.setCity(city);
      // FIXME: retrieve coutry from rdf data
      String country = "Germany";

      query.setCountry(country);


      NominatimSearchRequest request = new NominatimSearchRequest();
      request.setQuery(query);
      request.setLimit(1);

      List<Address> siteAddressList = nominatimClient.search(request);

      // check if we have a result
      if (siteAddressList.isEmpty()) {

        log.warn("No coordinates found for site: {}", siteUri);

      } else {

        // limit was set to one, so we only consider first list element
        Address address = siteAddressList.get(0);

        Resource siteResource = coordinatesModel.createResource(siteUri);

        Literal geo_long = coordinatesModel.createTypedLiteral(new Float(String.valueOf(address.getLongitude())));
        Literal geo_lat = coordinatesModel.createTypedLiteral(new Float(String.valueOf(address.getLatitude())));
        Literal ogc_point = coordinatesModel.createTypedLiteral("POINT(" + String.valueOf(address.getLongitude())
          + " " + String.valueOf(address.getLatitude()) + ")", OGC_WKT_LITERAL);
        coordinatesModel.add(siteResource, GEO_LONG, geo_long);
        coordinatesModel.add(siteResource, GEO_LAT, geo_lat);
        coordinatesModel.add(siteResource, OGC_AS_WKT, ogc_point);
      }

      // create Nominatim API request to get coordinates

      //ExtendedSearchQuery queryCity = new ExtendedSearchQuery();
      //queryCity.setPostalCode(postalCode);
      //queryCity.setCity(city);
      //queryCity.setCountry(country);
      SimpleSearchQuery requestCity = new SimpleSearchQuery(city + ", " + country);
      NominatimSearchRequest r = new NominatimSearchRequest();
      r.setPolygonFormat(PolygonFormat.TEXT);
      r.setQuery(requestCity);
      List<Address> adrList = nominatimClient.search(r);
      if (adrList.isEmpty()) {
      } else {
    	  Address a = adrList.get(0);
    	  //PolygonPoint[] pps = a.getPolygonPoints();
    	  //log.info("Adr: " + a.getWkt());
    	  log.info("Adr: " + a.getLongitude());
      }
    }

    inputModel.add(coordinatesModel);

    RDFDataMgr.write(new FileOutputStream(this.properties.getOutputFile()), inputModel,
        RDFFormat.TURTLE);
  }
}
