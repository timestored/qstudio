/ Tests for dbmaint scripts
/ Tests for each function on partitioned, segmented, splayed and in-memory tables

//TODO segmented example
//TODO nested column daat example
//TODO Test additional functions 
//TODO GUI breaks as element is not found
//TODO return log output of each action
//TODO Test on linux

system "d .dbmTest";

/#################### Constants ####################

/ table of example data
n:300;
T:([] date:asc n#2012.01.01+til 10; time:n#09:00t+60000*til 10; sym:n#`A`B`C; price:`float$til n; size:1000+til n; nnum:n#enlist 1 2h);
/ empty folder that will be used for testing
D:`:/temp/delme;
E:`:/temp/empty;

/#################### Helper Functions ####################

/ log an argument to screen
lg:{1 string[.z.t],"  ",$[type[x] in 10 -10h; x; .Q.s x],"\r\n"; x};

// delete all files from within this folder
// @throws emptyFail exception if non-successful
emptyDir:{ [path]
   win:.z.o like "w*";
   run:{@[{system lg x};x;`]};
   if[win;
        a:ssr[1_string path; "/"; "\\"];
        run each ("rmdir /S /Q ",a;"mkdir ",a);];
   if[not win; run "rm -rf ",(1_string path),"/*";];
   $[count key path; 'emptyFail; path]};


/#################### Create Database Functions

/ remove all global variables, create empty folder at path specified
prepDir:{
    delete from `.;
    d:` sv D,`$$[10h~type x;x;(8?.Q.a)]; // use random if none supplied
    reload E;
    emptyDir d};

reload:{ p:$[-11h~type x;x;`:.]; system "l ",1_string p; p };

/ load a splayed DB in a folder with supplied name, and reload kdb.
loadSDB:{ 
    d:prepDir[x];
    (a:` sv d,`$"/t/") set .Q.en[d] T; // create splayed t
    reload d};

loadMDB:{ reload E; @[`.;`t;:;T]; E};

/ load a paritioned DB in a folder with supplied name, and reload kdb.
loadPDB:{
    p:prepDir[x];
    f:{[p;d] (` sv p,`$(string d;"/t/")) set .Q.en[p] delete date from select from T where date=d}[p;];
    f each exec date from T;
    reload p};
  
/ load a segmented DB in a folder with supplied name, and reload kdb.
/ @param n - number of rows in each partition
loadSegDB:{ [pth; n]
    p:prepDir[pth];
    / the table we will write to each folder
    tbl:([] time:asc 09:00t+n?8*60*60*1000; sym:n#`A`B`C; price:`float$til n; size:1000+til n);
    f:{[tbl; p; seg; d] (` sv p,seg,`$(string d;"/t/")) set .Q.en[p] tbl}[tbl; p;];
    f[`d0;] each 2014.01.01+til 10;
    f[`d1;] each 2014.01.11+til 10;
    fh:hopen ` sv p,`par.txt;
    neg[fh] each {1 _ string ` sv x,y}[p;] each `d0`d1;
    hclose fh;
    reload p};  
        
/#################### delete, reload then assert the outcome of our actions
gett:{ @[`.;`t]};  

/ .unittest.assertEquals
aEq:{.qunit.assertEquals[x;y;z]};

addTabU:{ .dbm.addTable[`u;([] a:1 2j; b:3 4.1f)]; u:@[`.;`u];
    aEq[type u; 98h; "u is a table"];
    aEq[cols u; `date`a`b; "new partitioned table was added"];
    aEq[count u; 0; "empty as expected"]; show `p9;
    aEq[exec t from select t from meta u; "djf"; "column type matches"]};  
    
addtabUError:{ 
    u:([] a:1 2j; b:3 4.1f);
    msg:"exception adding table to non-partitioned DB";
    .qunit.assertError[ .dbm.addTable[`u;]; u; msg]};    
    
addT:{ .dbm.addCol[`t;`newInt; 0Ni]; t:gett[];
    aEq[asc cols t; asc cols[T],`newInt; "newInt column was added"];
    aEq[select from t; update newInt:0Ni from T; "data matches"]};  
    
delT:{ .dbm.deleteCol[`t;`time]; t:gett[];
    aEq[cols t; cols[T]  except `time; "time column was removed"];
    aEq[select from t; delete time from T; "data matches"]};        
    
copT:{ .dbm.copyCol[`t;`price;`p]; t:gett[];
    aEq[asc cols t; asc cols[T],`p; "p column was added"];
    aEq[select from t; update p:price from T; "value of copied column p matches price"]}; 

renT:{ .dbm.renameCol[`t;`price;`p]; t:gett[];
    aEq[asc except[cols[T];`price],`p; asc cols t; "price column was renamed p"];
    aEq[delete price from update p:price from T; select from t; "renamed table matches"]};

attgT:{ .dbm.setAttrCol[`t;`sym;`g]; t:gett[];
    aEq[exec first a from meta[t] where c=`sym; `g; "sym column is grouped"];
    aEq[select from t; T; "table unchanged"]};


nestedAddCopy:{ 
    .dbm.addCol[`t;`newNestJ; enlist 1 2j]; t:gett[];
    aEq[asc cols t; asc cols[T],`newNestJ; "newNestJ column was added"];
    aEq[first exec t from meta t where c=`newNestJ; "J"; "meta big J as expected"];
    aEq[exec newNestJ from select first newNestJ from t; enlist 1 2j; "data matches"];
    .dbm.copyCol[`t;`newNestJ; `bigJ]; t:gett[];
    aEq[`int$first exec a from select a:sum not bigJ~'newNestJ from t; 0i; "copied column matches"]};

/#################### TESTS ####################
/ need to cover
/ mem, splayed, partitioned, segmented
/ delete, copy, rename, set attrib

/ unused as yet
/ testMemAddTabU:{ loadMDB[]; addtabUError[]};
/ testSplayAddTabU:{ loadSDB["splayAddTabU"]; addtabUError[]};
/ testPartiAddTabU:{ loadPDB["partiAddTabU"]; addTabU[]};

/ nested data
testNestedPartiAddCopy:{ loadPDB["nestedPartiAddCopy"]; nestedAddCopy[]};
testNestedMemAddCopy:{ loadMDB[]; nestedAddCopy[]};
    
testMemAdd:{ loadMDB[]; addT[]};
testSplayAdd:{ loadSDB["splayAdd"]; addT[]};
testPartiAdd:{ loadPDB["partiAdd"]; addT[]};

/ the deletes check the actual disk as there was a bug where
/ files were locked and couldn't be deleted.
        
testMemDelete:{ loadMDB[]; delT[]};
testSplayDelete:{ p:loadSDB["splayDelete"]; delT[];
    .qunit.assertTrue[not `time in key ` sv p,`t; "time column does not exist on disk"]};
testPartiDelete:{ p:loadPDB["partiDelete"]; delT[];
    dts:`$string exec distinct date from T;
    r:{not `time in key ` sv x,y,`t}[p;] each {x!x} dts;
    .qunit.assertTrue[all r; "time column does not exist on disk"]};

testMemCopy:{ loadMDB[]; copT[]};
testSplayCopy:{ loadSDB["splayCopy"]; copT[]};
testPartiCopy:{ loadPDB["partiCopy"]; copT[]};

testMemRename:{  loadMDB[];  renT[]  };
testSplayRename:{   loadSDB["splayRename"]; renT[]};
testPartiRename:{   loadPDB["partiRename"]; renT[]};

testMemAttribG:{ loadMDB[];   attgT[] };
testSplayAttribG:{ loadSDB["splayAttribG"]; attgT[]};
testPartiAttribG:{ loadPDB["partiAttribG"]; attgT[]};

/ // partitioned case special cases
testPartiDeletePColumn:{ loadPDB["partiFail"];  .qunit.assertError[.dbm.deleteCol[`t;]; `date; "cant delete par col"] };
testPartiCopyIntoPColumn:{   loadPDB["partiFail"];  .qunit.assertError[.dbm.copyCol[`t;`price;]; `date; "cant copy over par col"] };
testPartiRenamePColumn:{ loadPDB["partiFail"];  .qunit.assertError[.dbm.renameCol[`t;`date;]; `p; "cant rename par col"] };
testPartiAttribGPColumn:{loadPDB["partiFail"];  .qunit.assertError[.dbm.setAttrCol[`t;`date;]; `g; "cant set attribute on par col"] };

/ illegal column name
testRenameBadColumnName:{ loadMDB[]; .qunit.assertError[.dbm.renameCol[`t;`date;]; `123; "cant name column with just numbers"] };
testRenameNonexistantColumn:{ loadMDB[]; .qunit.assertError[.dbm.renameCol[`t;`d123;]; `aaaa; "cant rename non-existant column"] };

/ segmented is all grouped into one test that does a sequence of actions
/ this way we reduce overhead of making DB and test combined functionality.
testSegMultiActions:{ loadSegDB["segMultiActions"; 300000]; 
    knownCols: asc `date`time`sym`price`size;
    .dbm.addCol[`t;`newInt; 0Ni]; t:gett[];
    aEq[asc cols t; asc knownCols,`newInt; "newInt column was added"];
    aEq[count select newInt from t where not null newInt; 0; "newInt data matches"];
    .dbm.renameCol[`t;`newInt; `newerInt]; t:gett[];
    aEq[asc cols t; asc knownCols,`newerInt; "newInt was renamed to newerInt"];
    .dbm.copyCol[`t; `size; `sz]; t:gett[];
    dt:first 1?exec date from select distinct date from t;
    aEq[select size from t where date=dt; select size:sz from t where date=dt; "copied sz matches"];
    .dbm.deleteCol[`t;] each `sz`newerInt; t:gett[];
    aEq[asc cols t; asc knownCols; "deleted sz and newerInt"]};
/ 
/ /**      Edge Cases ******/
/ this test currently fails.
/ testPartiCopyFromPColumn:{   loadPDB["partiCopyFromPColumn"];  
/     .dbm.copyCol[`t;`date;`dd]; t:gett[];
/     aEq[asc cols t; asc cols[T],`dd; "dd column was added"];
/     aEq[select from t; update dd:date from T; "value of copied column dd matches date"] };
