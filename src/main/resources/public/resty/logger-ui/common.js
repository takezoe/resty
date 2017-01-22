Vue.component('log-console-content', {
  props: ['active'],
  template:
    '<div>' +
      '<nav class="navbar navbar-inverse navbar-fixed-top">' +
        '<div class="container">' +
          '<div class="navbar-header"><a class="navbar-brand" href="javascript:void(0);">Log console</a></div>' +
          '<div id="navbar" class="collapse navbar-collapse">' +
            '<ul class="nav navbar-nav">' +
              '<li :class="{active: active == \'loglevel\'}"><a href="index.html">Log level</a></li>' +
              '<li :class="{active: active == \'download\'}"><a href="download.html">Download</a></li>' +
              '<li :class="{active: active == \'tail\'}"><a href="tail.html">Tail</a></li>' +
            '</ul>' +
          '</div>' +
        '</div>' +
      '</nav>' +
      '<div class="container">' +
        '<div style="padding-top: 80px;"><slot></slot></div>' +
      '</div>' +
    '</div>'
});
