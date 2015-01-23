// Variables
var map = null; // Set during initialization
var suppliers = []; // Map from a supplier ID to a marker
var connections = []; // Map from connection ID to a polyline
var selectedSupplier = null; // The ID of the selected supplier
var selectedConnection = null; // The ID of the selected connection

// Constants
var supplierIcon = '../assets/images/supplier.png';
var supplierSelectedIcon = '../assets/images/supplier-selected.png';
var connectionIcon = {
  path: google.maps.SymbolPath.FORWARD_OPEN_ARROW
};

function initialize() {
  // Initialize map
  var mapOptions = {
    center: new google.maps.LatLng(50.708406, 10.382866),
    zoom: 6,
    mapTypeId: google.maps.MapTypeId.ROADMAP
  };
  map = new google.maps.Map(document.getElementById("map-content"), mapOptions);

  // Draw addresses
  showSuppliers();
  showConnections();

  // Stream deliveries
  $('#deliveryStream').html('<iframe src="deliveryStream" frameborder="0"></iframe>');
}

function addSupplier(id, title, latitude, longitude, dueParts) {
  var marker = new MarkerWithLabel({
    position: new google.maps.LatLng(latitude, longitude),
    title: title,
    icon: supplierIcon,
    labelContent: "" + dueParts,
    labelClass: "supplier_label"
  });

  google.maps.event.addListener(marker, 'click', function(event) { selectSupplier(id) });
  suppliers[id] = marker;
}

function addConnection(id, senderLat, senderLon, receiverLat, receiverLon) {
  //Draw line
  var line = new google.maps.Polyline({
    path: [new google.maps.LatLng(senderLat, senderLon),
           new google.maps.LatLng(receiverLat, receiverLon)],
    strokeColor: '#0000FF',
    strokeOpacity: 0.5,
    strokeWeight: 1.5,
    icons: [{
      icon: connectionIcon,
      offset: '100%'
    }]
  });

  google.maps.event.addListener(line, 'click', function(event) { selectConnection(id) });
  line.setMap(map);
  connections[id] = line;
}

function addOrder(supplierId, connectionId, dueParts) {
  // Update dueOrders for supplier
  suppliers[supplierId].setOptions({ labelContent: "" + dueParts });
  // Flash connection line
  connections[connectionId].setOptions({ strokeColor: '#FF0000' });
  setTimeout(function() {
    connections[connectionId].setOptions({  strokeColor: '#0000FF' });
  }, 1000);
}

function addShipping(supplierId, connectionId, dueParts) {
  // Update dueOrders for supplier
  suppliers[supplierId].setOptions({ labelContent: "" + dueParts });
  // Flash connection line
  connections[connectionId].setOptions({  strokeColor: '#00FF00' });
  setTimeout(function() {
    connections[connectionId].setOptions({ strokeColor: '#0000FF' });
  }, 1000);
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
  $.get("map/loadSuppliers", function(data) {
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
  var uri = "map/loadConnections";
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

/**
 * Highlights a supplier marker on the map.
 */
function selectSupplier(supplierId) {
  // Hightlight selected supplier
  if(selectedSupplier !== null)
    suppliers[selectedSupplier].setIcon(supplierIcon);
  suppliers[supplierId].setIcon(supplierSelectedIcon);
  // Update selected supplier
  selectedSupplier = supplierId;
  // Show all connections from/to this supplier
  showConnections(supplierId);
  // Show supplier properties in the property widget
  $.get("supplier/" + supplierId, function(data) {
    $('#property-content' ).html(data)
  });

  // load stock data from suppliers xybermotive system
  loadXybermotive(supplierId)

  // Reload metrics
  reloadMetrics();
}

var xyRefreshTimer;

function loadXybermotive(supplierId) {
  $.get("xybermotive/" + supplierId, function(data) {
    $('#xybermotive-content' ).html(data)
  });

  clearTimeout(xyRefreshTimer);
  xyRefreshTimer = setTimeout(function() { loadXybermotive(supplierId); }, 5000);
}


/**
 * Highlights a connection line on the map.
 */
function selectConnection(connectionId) {
  // Highlight selected connection
  if(selectedConnection !== null)
    connections[selectedConnection].setOptions({ strokeWeight: 1.5, strokeOpacity: 0.5 });
  connections[connectionId].setOptions({ strokeWeight: 2.5, strokeOpacity: 1.0 });
  // Update selected connection
  selectedConnection = connectionId;
  // Show connection properties in the property widget
  $.get("delivery/" + connectionId, function(data) {
    $('#property-content').html(data)
  })
}

var refreshTimer;

function refreshMetrics() {
  clearTimeout(refreshTimer);
  refreshTimer = setTimeout(function() { reloadMetrics(); }, 2000);
}

function reloadMetrics() {
  $.get("metrics?supplierId=" + selectedSupplier, function(data) {
    $('#metrics-content' ).html(data)
  })
}

google.maps.event.addDomListener(window, 'load', initialize);