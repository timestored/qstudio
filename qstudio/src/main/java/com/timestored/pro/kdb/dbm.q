/ Database maintenance scripts for altering columns
/ Based on code from: pdurkan@firstderivatives.com

system "d .dbm";

l:el:("  ";"  "); / log
lg:{a:string[.z.t],"  ",$[type[x] in 10 -10h; x; .Q.s x],"\r\n"; l::l,enlist a; 1 a; x};

// OS level commands

run:{system lg x};
WIN:.z.o in`w32`w64;
pth:{p:$[10h=type x;x;string x];if[WIN;p[where"/"=p]:"\\"];(":"=first p)_ p};
cpy:{run$[WIN;"copy /v /z ";"cp "],pth[x]," ",pth y};
del:{run$[WIN;"del ";"rm "],pth x};
/ x - path, y - oldName, z - new name. linux must specify both as full paths
ren:{run $[WIN;"ren ";"mv "],pth[` sv x,y]," ",pth $[WIN;z;` sv x,z]};
here:{hsym`$run$[WIN;"cd";"pwd"]};
reload:{ p:$[-11h~type x;x;`:.]; system "l ",1_string p; p };

/ check if it's the partitioned column
isPCol:{ [table;col] (.Q.qp @[`.;table]) and col~first cols table };

add1col:{[tabledir;colname;defaultvalue]
 if[not colname in ac:allcols tabledir;
  lg "adding column ",(string colname)," (type ",(string type defaultvalue),") to `",string tabledir;
  num:count get(`)sv tabledir,first ac;
  .[(`)sv tabledir,colname;();:;num#defaultvalue];
  @[tabledir;`.d;,;colname]]};

allcols:{[tabledir]get tabledir,`.d};

allpaths:{[dbdir;table]
 files:key dbdir;
 if[table in files;:(),` sv dbdir,table]; // splayed
 if[any files like"par.txt";:raze allpaths[;table]each hsym each`$read0(`)sv dbdir,`par.txt];
 files@:where files like"[0-9]*";(`)sv'dbdir,'files,'table};

copy1col:{[tabledir;oldcol;newcol]
 if[(oldcol in ac)and not newcol in ac:allcols tabledir;
  lg "copying ",(string oldcol)," to ",(string newcol)," in `",string tabledir;
  cpy[(`)sv tabledir,oldcol;(`)sv tabledir,newcol];
  if[(onc:`$string[oldcol],"#") in key tabledir; cpy[` sv tabledir,onc; ` sv tabledir,(`$string[newcol],"#")]];
  @[tabledir;`.d;,;newcol]]};

delete1col:{[tabledir;col]
 if[col in ac:allcols tabledir;
  lg "deleting column ",(string col)," from `",string tabledir;
  del[(`)sv tabledir,col];@[tabledir;`.d;:;ac except col]]};

enum:{[tabledir;val]if[not 11=abs type val;:val];`sym set$[type key p:` sv tabledir,`sym;get p;0#`];e:`sym?val;.[p;();:;sym];e};


find1col:{[tabledir;col]
 $[col in allcols tabledir;
   lg "column ",string[col]," (type ",(string first"i"$read1((`)sv tabledir,col;8;1)),") in `",string tabledir;
   lg "column ",string[col]," *NOT*FOUND* in `",string tabledir]};

fix1table:{[tabledir;goodpartition;goodpartitioncols]
 if[count missing:goodpartitioncols except allcols tabledir;
  lg "fixing table `",string tabledir;{add1col[x;z;0#get y,z]}[tabledir;goodpartition]each missing]};

fn1col:{[tabledir;col;fn]
 if[col in allcols tabledir;
  oldattr:-2!oldvalue:get p:tabledir,col;
  newattr:-2!newvalue:fn oldvalue;		
  if[$[not oldattr~newattr;1b;not oldvalue~newvalue];
   lg "resaving column ",(string col)," (type ",(string type newvalue),") in `",string tabledir;
   oldvalue:0;.[(`)sv p;();:;newvalue]]]};

reordercols0:{[tabledir;neworder]
 if[not((count ac)=count neworder)or all neworder in ac:allcols tabledir;'`order];
 lg "reordering columns in `",string tabledir;
 @[tabledir;`.d;:;neworder]};

rename1col:{[tabledir;oldname;newname]
 if[(oldname in ac)and not newname in ac:allcols tabledir;
  lg "renaming ",(string oldname)," to ",(string newname)," in `",string tabledir;
  $[(meta tabledir)[oldname;`t]="C";ren[tabledir; `$(string oldname),"#";`$(string newname),"#"] ; ::];
  ren[tabledir; oldname;newname];
  @[tabledir;`.d;:;.[ac;where ac=oldname;:;newname]]]};

// this ren needs fixed to specify path
ren1table:{[old;new] lg "renaming ",(string old)," to ",string new;ren[old;new];};

validcolname:{$[all(sx:string x)in .Q.an;sx[0]in .Q.a,.Q.A;0b]};

/////////////////////  PUBLIC    ///////////////////////////////////////////

addCol:{[table; colname; defaultvalue] c:colname; v:defaultvalue;
    if[not validcolname c;'(`)sv c,`invalid.colname];
    $[0~.Q.qp @[`.;table];
        ![table;();0b;enlist[c]!enlist (#;(count;table); v)]; // in memory, # is needed for nested columns
        [add1col[;c;enum[`:.;v]]each allpaths[`:.;table]; reload[];]];
    lg "Column Added."};

castcol:{[dbdir;table;col;newtype] / castcol[thisdb;`trade;`size;`short]
 fncol[dbdir;table;col;newtype$]};

 
copyCol:{[table; oldcol; newcol] n:newcol;
 if[not validcolname n;'` sv n,`invalid.newname];
 if[isPCol[table;n]; 'cantCopyPCol];
    $[0~.Q.qp @[`.;table];
        ![table;();0b;enlist[n]!enlist oldcol]; // in memory copy
        [copy1col[;oldcol;n]each allpaths[`:.;table]; reload[];]]; 
 lg "Column Copied."};

deleteCol:{[table;col] 
 if[isPCol[table;col]; 'cantDeletePCol];
    $[0~.Q.qp @[`.;table];
        ![table;();0b;enlist col]; // in memory delete
        [delete1col[;col]each allpaths[`:.;table]; reload[];]];
 lg "Column Deleted."};

findcol:{[dbdir;table;col] / findcol[`:/k4/data/taq;`trade;`iz]
 find1col[;col]each allpaths[dbdir;table];};

/ adds missing columns, but DOESN'T delete extra columns - do that manually
fixtable:{[dbdir;table;goodpartition] / fixtable[`:/k4/data/taq;`trade;`:/data/taq/2005.02.19]
 fix1table[;goodpartition;allcols goodpartition]each allpaths[dbdir;table]except goodpartition;};

fncol:{[dbdir;table;col;fn] / fncol[thisdb;`trade;`price;2*]
 fn1col[;col;fn]each allpaths[dbdir;table];};

listcols:{[dbdir;table] / listcols[`:/k4/data/taq;`trade]
 allcols first allpaths[dbdir;table]};

renameCol:{[table;oldname;newname] o:oldname; n:newname; tb:table;
 if[not validcolname n;'` sv n,`invalid.newname];
 if[isPCol[tb;o]; 'cantRenamePCol];
 $[0~.Q.qp @[`.;tb];
    [copyCol[tb;o;n]; deleteCol[tb;o];]; // in memory delete
    [rename1col[;o;n]each allpaths[`:.;tb]; reload[];]];
    lg "Column Renamed."};
    
reordercols:{[dbdir;table;neworder] / reordercols[`:/k4/data/taq;`trade;reverse cols trade]
 reordercols0[;neworder]each allpaths[dbdir;table];};


/ @param newattr one of `p`s`u`g`
setAttrCol:{[table;col;newattr] 
    $[0~.Q.qp @[`.;table];
        @[table;col;newattr#]; // in memory apply attrib
        $[isPCol[table;col]; 
            'cantSetAttrOnPColumn; 
            [fncol[`:.;table;col;newattr#]; reload[];]]]; 
    lg "Set Attribute on Column."};

/ .dbm.addTable[`ff; ([] a:1 2 3)]
addTable:{ [tablename;table]
 if[not any 1b~'{.Q.qp value x} each system "a"; 'partitionedDBonly];
 add1table[`:.;;table]each 0N!allpaths[dbdir;tablename];};

add1table:{[dbdir;tablename;table]
 lg "adding ",string tablename;
 @[tablename;`;:;.Q.en[dbdir]0#table];
 @[tablename;`.d;:;cols table]};

rentable:{[dbdir;old;new] / rentable[`:.;`trade;`transactions]
 ren1table'[allpaths[dbdir;old];allpaths[dbdir;new]];};
