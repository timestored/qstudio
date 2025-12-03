{ [noArg] 
    
    tabs:asc (system "a") except `timestoredDescriptions;
    if[not count tabs; '"notables"];
    
    // function that takes a table and joins on our documentation if possible
    docLookup:{x};
    // if description table is available and populated use it for lookup
	if[(nm:`timestoredDescriptions) in key `.; 
        dtbl:$[.Q.qt v:value nm; select first description by table,column from v; ()];
    	if[count dtbl; 
            docLookup:{ [dtbl; srcTbl] $[any 2<count each exec description from r:srcTbl lj dtbl; r; srcTbl]}[dtbl;];
        ];
    ];
    
    // report table with one row per table in database
    getOverview:{ [tabs; docLookup] 
        gett:{
        	// replace empty wqith blank, 1 dont show comma, many use -3!
    		formatVals:{@[{$[0=count x;"";$[(11h=type x) and 1=count x;"`",string first x;80 sublist trim -3!`#x]]};x;"  "]};
        	getFmt:{((0;0b;1b)!`memory`splayed`partitioned) .Q.qp[x]};
        	cnames:`table`count`format`columns`keys;
        	cnames!(x; count value x; getFmt x;formatVals asc cols x; k:asc keys x)};
        t:`table xasc gett each tabs;
        / remove table format column if always the same
        t:$[1=count distinct t `format; ``format _ t; t];
        / remove key column if always empty
        t:$[all 0=count each t `keys; ``keys _ t; t];
        / fake column column to allow joining, that is later removed
        ``column _ docLookup update column:` from t};
    
    // @return  dictionary from tablename -> reportTable with one row per column
    getDetails:{ [tabs; docLookup]
        sampleTbl:{ [tbl]
            getT:{ [tbl; cnt] 
            	vt:value tbl; 
            	if[cnt>count vt; :() xkey  vt];
            	() xkey  $[.Q.qp vt; .Q.ind[tbl;til cnt];neg[cnt]?vt]};
            / protected stringing of distinct values to ensure always returns
    		formatVals:{@[{$[0=count x;"";$[(11h=type x) and 1=count x;"`",string first x;80 sublist trim -3!`#x]]};x;"  "]};
            formatVals each {asc distinct x} each flip getT[tbl; 100200]};
            
        f:{ [sampleTbl; docLookup; tabName]
        	typeMap: "bcdefhijmnpstuvxz"!`boolean`char`date`real`float`short`int`long`month`timespan`timestamp`symbol`time`minute`second`byte`datetime;
        	t:select tabName,c,(`$'t)^typeMap t,a from 0!meta tabName;
        	t:`table`column`typ`attribute xcol t;
            update sample:sampleTbl[tabName] column from ``table _ docLookup t};
        @[f[sampleTbl;docLookup;];;()] each {x!x} tabs};
        
    (getOverview[tabs; docLookup]; getDetails[tabs;docLookup])
    }[]