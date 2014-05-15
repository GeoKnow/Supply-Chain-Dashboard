var map = null; // Set during initialization
var suppliers = []; //Map from a supplier ID to a marker
var connections = []; //Map from connection ID to a polyline

function initialize() {
  // Initialize map
  var mapOptions = {
    center: new google.maps.LatLng(52.423, 10.787), // Wolfsburg
    zoom: 8,
    mapTypeId: google.maps.MapTypeId.ROADMAP
  };
  map = new google.maps.Map(document.getElementById("map-content"), mapOptions);

  // Draw addresses
  showSuppliers();
  showConnections();

  // Stream deliveries
  $('#deliveryStream').html('<iframe src="/deliveryStream"></iframe>');
}

function addSupplier(id, title, latitude, longitude) {
  var marker = new MarkerWithLabel({
    position: new google.maps.LatLng(latitude, longitude),
    title: title,
    labelContent: "0",
    labelClass: "supplier_label"
  });

  google.maps.event.addListener(marker, 'click', function(event) { selectSupplier(id) });
  suppliers[id] = marker;
}

function addConnection(id, senderLat, senderLon, receiverLat, receiverLon) {
  var arrowIcon = {
    path: google.maps.SymbolPath.FORWARD_OPEN_ARROW
  };

  //Draw line
  var line = new google.maps.Polyline({
    path: [new google.maps.LatLng(senderLat, senderLon),
           new google.maps.LatLng(receiverLat, receiverLon)],
    strokeColor: '#0000FF',
    strokeOpacity: 0.8,
    strokeWeight: 2,
    icons: [{
      icon: arrowIcon,
      offset: '100%'
    }]
  });

  google.maps.event.addListener(line, 'click', function(event) { selectDelivery(id) });
  line.setMap(map);
  connections[id] = line;
}

function addOrder(supplierId, connectionId, dueParts) {
  console.log("Received order " + connectionId + ". Due parts: " + dueParts);
  suppliers[supplierId].setOptions({ labelContent: "" + dueParts });
  connections[connectionId].setOptions({ strokeColor: '#FF0000' });
}

function addShipping(supplierId, connectionId, dueParts) {
  console.log("Received shipping " + connectionId + ". Due parts: " + dueParts);
  suppliers[supplierId].setOptions({ labelContent: "" + dueParts });
  connections[connectionId].setOptions({ strokeColor: '#00FF00' });
}

function hideSuppliers() {
  if (typeof suppliers !== 'undefined') {
    for (var s in suppliers) {
      if (suppliers.hasOwnProperty(s)) {
        suppliers[s].setMap(null);
      }
    }
    suppliers = [];
  }
}

function showSuppliers() {
  $.get("/map/loadSuppliers", function(data) {
    // Remove existing address markers
    hideSuppliers();
    // Load new address markers
    jQuery.globalEval(data);
    // Add all address markers to the map
    for (var s in suppliers) {
      if (suppliers.hasOwnProperty(s)) {
        suppliers[s].setMap(map);
      }
    }
  });
}

function hideConnections() {
  if (typeof connections !== 'undefined') {
    for (var c in connections) {
      if (connections.hasOwnProperty(c)) {
        connections[c].setMap(null);
      }
    }
    connections = [];
  }
}

function showConnections(supplierId, contentType) {
  var uri = "/map/loadConnections";
  if(supplierId)
    uri += "?supplierId=" + supplierId;
  if(contentType && contentType != "all")
    uri += "?contentType=" + contentType;

  $.get(uri, function(data) {
    // Remove existing delivery lines
    hideConnections();
    // Load new delivery lines
    jQuery.globalEval(data);
    // Add all lines to the map
    for (var c in connections) {
      if (connections.hasOwnProperty(c)) {
        connections[c].setMap(map);
      }
    }
  });
}

function selectSupplier(addressId) {
  showConnections(addressId);
  $.get("/supplier/" + addressId, function(data) {
    $('#property-content' ).html(data)
  })
}

function selectDelivery(deliveryId) {
  $.get("/delivery/" + deliveryId, function(data) {
    $('#property-content' ).html(data)
  })
}

google.maps.event.addDomListener(window, 'load', initialize);