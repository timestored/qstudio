/ Author / Copyright: Ryan Hamilton
/ reassign all functions in namespace,to have that namespace as their context.
/ @param ns symbol of namespace that contains test including preceding ".".
{ { [ns]
    if[ns~`.; :(::)]; // nothing needs done, return
    if[not (`$1_string ns) in key `; 'nsNoExist]; // cant find namespace
    n: 1_key ns; / names
     fn: {"." sv string x} each ns,'n; / fullnames
    funcIdx: where 100h={type value x} each fn;
    system "d ",string ns;
    value each {string[y],'":",/:string @[x; y]} [ns; n funcIdx];
    system "d .";
    n funcIdx} each (),x}