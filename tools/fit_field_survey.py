#!/usr/bin/env python3
"""Roh-Feldstatistik aus FIT-Originalen (lokal, nur lesend).

Eingabe: Ordner mit entpackten `*.fit` (aus der App: Daten > FIT-Originale exportieren).
Ausgabe: je (Nachricht, Feld) Anzahl, Dateien, Min/Max, haeufigste Werte. Die Ausgabe enthaelt echte Rohwerte und
gehoert NICHT ins Repo, in Tickets oder Logs. Eigener kleiner Parser (Definitionen, komprimierte Zeitstempel,
Entwicklerfelder); keine Interpretation, keine Einheiten.

  python3 tools/fit_field_survey.py ordner_mit_fit_dateien
"""
import struct, os, collections, json, sys
BT={0:('enum','B',1,0xFF),1:('sint8','b',1,0x7F),2:('uint8','B',1,0xFF),0x83:('sint16','h',2,0x7FFF),0x84:('uint16','H',2,0xFFFF),
    0x85:('sint32','i',4,0x7FFFFFFF),0x86:('uint32','I',4,0xFFFFFFFF),7:('string',None,1,None),0x88:('float32','f',4,None),0x89:('float64','d',8,None),
    0x0A:('uint8z','B',1,0),0x8B:('uint16z','H',2,0),0x8C:('uint32z','I',4,0),0x0D:('byte',None,1,None),0x8E:('sint64','q',8,0x7FFFFFFFFFFFFFFF),0x8F:('uint64','Q',8,0xFFFFFFFFFFFFFFFF),0x90:('uint64z','Q',8,0)}
def parse(path):
    b=open(path,'rb').read()
    hs=b[0]; dsz=struct.unpack('<I',b[4:8])[0]; end=hs+dsz; p=hs; defs={}; out=[]
    while p<end:
        h=b[p]; p+=1
        if h&0x80:
            lt=(h>>5)&3; d=defs[lt]; ts_off=h&0x1F
        elif h&0x40:
            lt=h&15; p+=1; arch=b[p]; p+=1; e='>' if arch else '<'
            g=struct.unpack(e+'H',b[p:p+2])[0]; p+=2; nf=b[p]; p+=1
            fl=[]
            for _ in range(nf): fl.append((b[p],b[p+1],b[p+2])); p+=3
            dev=[]
            if h&0x20:
                nd=b[p]; p+=1
                for _ in range(nd): dev.append((b[p],b[p+1],b[p+2])); p+=3
            defs[lt]=dict(g=g,e=e,f=fl,dev=dev); continue
        else:
            lt=h&15; d=defs[lt]; ts_off=None
        rec={}
        for (n,sz,t) in d['f']:
            raw=b[p:p+sz]; p+=sz
            rec[n]=(t,sz,raw)
        for (n,sz,i) in d['dev']:
            raw=b[p:p+sz]; p+=sz; rec[('dev',i,n)]=(0x0D,sz,raw)
        out.append((d['g'],d['e'],rec,ts_off))
    return out
def val(t,sz,raw,e):
    info=BT.get(t)
    if not info: return ('raw',raw.hex())
    name,fmt,esz,inv=info
    if name=='string': return raw.split(b'\0')[0].decode('utf8','replace')
    if name=='byte' or fmt is None: return ('hex',raw.hex())
    if sz%esz: return ('hex',raw.hex())
    vals=struct.unpack(e+fmt*(sz//esz),raw)
    vals=[None if (inv is not None and v==inv) else v for v in vals]
    return vals[0] if len(vals)==1 else tuple(vals)
def main(root):
    stats=collections.defaultdict(lambda:{'n':0,'files':set(),'vals':collections.Counter(),'type':set(),'size':set(),'inv':0,'min':None,'max':None,'nonnum':0})
    for f in sorted(os.listdir(root)):
        for g,e,rec,_ in parse(os.path.join(root,f)):
            for k,(t,sz,raw) in rec.items():
                s=stats[(g,k)]; s['n']+=1; s['files'].add(f); s['type'].add(t); s['size'].add(sz)
                v=val(t,sz,raw,e)
                if v is None: s['inv']+=1; continue
                if isinstance(v,(int,float)):
                    s['min']=v if s['min'] is None else min(s['min'],v); s['max']=v if s['max'] is None else max(s['max'],v)
                else: s['nonnum']+=1
                if len(s['vals'])<5000: s['vals'][v if not isinstance(v,tuple) or len(v)<8 else v[:8]]+=1
    print(len(stats), 'Feldtypen,', sum(v['n'] for v in stats.values()), 'Werte')
    for (g, k), s in sorted(stats.items(), key=lambda x: (x[0][0], str(x[0][1]))):
        top = [(v if not isinstance(v, str) else v[:20], c) for v, c in s['vals'].most_common(3)]
        print(f"msg {g} feld {k} n={s['n']} dateien={len(s['files'])} ungueltig={s['inv']} min={s['min']} max={s['max']} verschieden={len(s['vals'])} haeufig={top}")

if __name__ == '__main__':
    import sys
    main(sys.argv[1] if len(sys.argv) > 1 else '.')
