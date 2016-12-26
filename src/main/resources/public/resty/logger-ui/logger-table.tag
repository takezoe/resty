<logger-table>
  <form id="form" class="form">
    <table class="table table-bordered">
      <thead>
      <tr>
        <th>Logger name</th>
        <th>Log level</th>
        <th>Update</th>
      </tr>
      </thead>
      <tbody>
      <tr each={items}>
        <td>{name}</td>
        <td>{effectiveLevel}</td>
        <td>
          <select class="logger" name={name}>
            <option each={item in levels} selected={level == item}>{item}</option>
          </select>
        </td>
      </tr>
      </tbody>
    </table>
    <input type="reset" value="Reset" class="btn btn-default">
    <input type="button" value="Save" class="btn btn-primary" onclick={save}>
  </form>
  this.levels = ['', 'OFF', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE', 'ALL'];

  this.items = [];
  var self = this;
  updateTable();

  function updateTable(){
    $.get('http://localhost:8080/logger/levels', function(items){
      self.items = items;
      self.update();
    });
  }

  save(event){
    var data = { loggers: [] };
    $('.logger').each(function(i, target){
      var e     = $(target);
      var name  = $(e).attr('name');
      var value = $(e).val();

      data.loggers.push({
        name: name,
        level: value == '' ? null : value
      });
    });

    $.ajax({
      type: 'post',
      url: 'http://localhost:8080/logger/levels',
      data: JSON.stringify(data),
      contentType: 'application/json',
      success: function(){
        updateTable();
        $('h1').after($(
          '<div class="alert alert-success alert-dismissible" role="alert">' +
          '<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
          'Succeed to save configuration!' +
          '</div>'
        ));
      },
      error: function(xhr, status, error){
        console.log(error);
      }
    });
  }
</logger-table>