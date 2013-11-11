var map = null // Set during initialization

function initialize() {
  // Initialize map
  var mapOptions = {
    center: new google.maps.LatLng(52.423, 10.787), // Wolfsburg
    zoom: 8,
    mapTypeId: google.maps.MapTypeId.ROADMAP
  }
  map = new google.maps.Map(document.getElementById("map-content"), mapOptions)

  // Draw addresses
  showAddresses()
}

function hideAddresses() {
  if (typeof addressMarkers !== 'undefined') {
    for (var i = 0; i < addressMarkers.length; i++) {
      addressMarkers[i].setMap(null)
    }
    addressMarkers = []
  }
}

function showAddresses() {
  $.get("/map/addresses", function(data) {
    // Remove existing address markers
    hideAddresses()
    // Load new address markers
    jQuery.globalEval(data)
    // Add all address markers to the map
    for (var i = 0; i < addressMarkers.length; i++) {
      //google.maps.event.addListener(addressMarkers[i], 'click', function(event) { showDeliveries(address.id) })
      addressMarkers[i].setMap(map)
    }
  })
}

function hideDeliveries() {
  if (typeof deliveryLines !== 'undefined') {
    for (var i = 0; i < deliveryLines.length; i++) {
      deliveryLines[i].setMap(null);
    }
    deliveryLines = []
  }
}

function showDeliveries(addressId) {
  var uri = "/map/deliveries"
  if(addressId)
    uri += "?addressId=" + addressId

  $.get(uri, function(data) {
    // Remove existing delivery lines
    hideDeliveries()
    // Load new delivery lines
    jQuery.globalEval(data)
    // Add all lines to the map
    for (var i = 0; i < deliveryLines.length; i++) {
      deliveryLines[i].setMap(map)
    }
  })
}

function selectAddress(addressId) {
  showDeliveries(addressId)
  $.get("/address/" + addressId, function(data) {
    $('#property-content' ).html(data)
  })
}

function selectDelivery(deliveryId) {
  $.get("/delivery/" + deliveryId, function(data) {
    $('#property-content' ).html(data)
  })
}

google.maps.event.addDomListener(window, 'load', initialize)