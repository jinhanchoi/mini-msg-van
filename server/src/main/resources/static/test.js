
function parallelApiCall(){
     $.when(
        $.get('/'),
        $.get('/?name=1'),
        $.get('/?name=2'),
        $.get('/?name=3'),
        $.get('/?name=1'),
        $.get('/?name=2'),
        $.get('/?name=3'),
        $.get('/?name=1'),
        $.get('/?name=2'),
        $.get('/?name=3'),
        $.get('/index2')
     ).then(function(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11){
        console.log(r1[0].message + " " + r2[0].message + " " + r3[0].message) + " " + r4[0].message;
     });
}