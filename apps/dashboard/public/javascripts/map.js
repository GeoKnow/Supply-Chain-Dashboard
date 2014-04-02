var map = null; // Set during initialization
var supplierMarkers = [];
var deliveryLines = [];

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

  // Stream deliveries
  $('#deliveryStream').html('<iframe src="/deliveryStream"></iframe>');
}

function addSupplier(id, title, latitude, longitude) {
  var marker = new google.maps.Marker({
    position: new google.maps.LatLng(latitude, longitude),
    title: title
  });

  google.maps.event.addListener(marker, 'click', function(event) { selectSupplier(id) });
  supplierMarkers.push(marker);
}

function addDelivery(id, senderLat, senderLon, receiverLat, receiverLon) {
  console.log("Adding delivery" + senderLat);

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
  deliveryLines.push(line);
}

function hideSuppliers() {
  if (typeof supplierMarkers !== 'undefined') {
    for (var i = 0; i < supplierMarkers.length; i++) {
      supplierMarkers[i].setMap(null)
    }
    supplierMarkers = []
  }
}

function showSuppliers() {
  $.get("/map/suppliers", function(data) {
    // Remove existing address markers
    hideSuppliers();
    // Load new address markers
    jQuery.globalEval(data);
    // Add all address markers to the map
    for (var i = 0; i < supplierMarkers.length; i++) {
      //google.maps.event.addListener(addressMarkers[i], 'click', function(event) { showDeliveries(address.id) })
      supplierMarkers[i].setMap(map);
    }
  });
}

function hideDeliveries() {
  if (typeof deliveryLines !== 'undefined') {
    for (var i = 0; i < deliveryLines.length; i++) {
      deliveryLines[i].setMap(null);
    }
    deliveryLines = [];
  }
}

function showDeliveries(supplierId, contentType) {
  var uri = "/map/deliveries";
  if(supplierId)
    uri += "?supplierId=" + supplierId;
  if(contentType && contentType != "all")
    uri += "?contentType=" + contentType;

  $.get(uri, function(data) {
    // Remove existing delivery lines
    hideDeliveries();
    // Load new delivery lines
    jQuery.globalEval(data);
    // Add all lines to the map
    for (var i = 0; i < deliveryLines.length; i++) {
      deliveryLines[i].setMap(map);
    }
  });
}

function selectSupplier(addressId) {
  showDeliveries(addressId)
  $.get("/supplier/" + addressId, function(data) {
    $('#property-content' ).html(data)
  })
}

function selectDelivery(deliveryId) {
  $.get("/delivery/" + deliveryId, function(data) {
    $('#property-content' ).html(data)
  })
}

google.maps.event.addDomListener(window, 'load', initialize)