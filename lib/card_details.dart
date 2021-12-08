class CardDetails {
  CardDetails(
      {this.the50,
      this.the57,
      this.the82,
      this.the84,
      this.the95,
      this.the9F06,
      this.the9F11,
      this.the5F2A,
      this.the9F09,
      this.the9B,
      this.the5F25,
      this.the9F36,
      this.the9F03,
      this.the9F07,
      this.the9C,
      this.the5F34,
      this.the5F24,
      this.the9F12,
      this.the5A,
      this.the9F10,
      this.the9F33,
      this.the9F40,
      this.the5F20,
      this.the9A,
      this.the9F26,
      this.the9F35,
      this.the9F02,
      this.the9F27,
      this.the9F34,
      this.the9F1A,
      this.the9F37,
      this.the9F1E,
      this.the9F21,
      this.the9F41,
      this.strTrack2,
      this.pan,
      this.expiry,
      this.src,
        this.cardPin,
        this.ksn

      });

  final String the50;
  final String the57;
  final String the82;
  final String the84;
  final String the95;
  final String the9F06;
  final String the9F11;
  final String the5F2A;
  final String the9F09;
  final String the9B;
  final String the5F25;
  final String the9F36;
  final String the9F03;
  final String the9F07;
  final String the9C;
  final String the5F34;
  final String the5F24;
  final String the9F12;
  final String the5A;
  final String the9F10;
  final String the9F33;
  final String the9F40;
  final String the5F20;
  final String the9A;
  final String the9F26;
  final String the9F35;
  final String the9F02;
  final String the9F27;
  final String the9F34;
  final String the9F1A;
  final String the9F37;
  final String the9F1E;
  final String the9F21;
  final String the9F41;
  String strTrack2;
  String cardPin ;
  String pan;
  String expiry;
  String src;
  String ksn ;

  factory CardDetails.fromJson(Map<String, dynamic> json) => CardDetails(
      the50: json["50"],
      the57: json["57"],
      the82: json["82"],
      the84: json["84"],
      the95: json["95"],
      the9F06: json["9F06"],
      the9F11: json["9F11"],
      the5F2A: json["5F2A"],
      the9F09: json["9F09"],
      the9B: json["9B"],
      the5F25: json["5F25"],
      the9F36: json["9F36"],
      the9F03: json["9F03"],
      the9F07: json["9F07"],
      the9C: json["9C"],
      the5F34: json["5F34"],
      the5F24: json["5F24"],
      the9F12: json["9F12"],
      the5A: json["5A"],
      the9F10: json["9F10"],
      the9F33: json["9F33"],
      the9F40: json["9F40"],
      the5F20: json["5F20"],
      the9A: json["9A"],
      the9F26: json["9F26"],
      the9F35: json["9F35"],
      the9F02: json["9F02"],
      the9F27: json["9F27"],
      the9F34: json["9F34"],
      the9F1A: json["9F1A"],
      the9F37: json["9F37"],
      the9F1E: json["9F1E"],
      the9F21: json["9F21"],
      the9F41: json["9F41"],
      strTrack2: json['strTrack2'],
      pan: json['pan'],
      expiry: json['expiry'],
      src: json['src'],
      cardPin: json['CardPin'],
      ksn: json['ksn'],
  );

  Map<String, dynamic> toJson() => {
        "50": the50,
        "57": the57,
        "82": the82,
        "84": the84,
        "95": the95,
        "9F06": the9F06,
        "9F11": the9F11,
        "5F2A": the5F2A,
        "9F09": the9F09,
        "9B": the9B,
        "5F25": the5F25,
        "9F36": the9F36,
        "9F03": the9F03,
        "9F07": the9F07,
        "9C": the9C,
        "5F34": the5F34,
        "5F24": the5F24,
        "9F12": the9F12,
        "5A": the5A,
        "9F10": the9F10,
        "9F33": the9F33,
        "9F40": the9F40,
        "5F20": the5F20,
        "9A": the9A,
        "9F26": the9F26,
        "9F35": the9F35,
        "9F02": the9F02,
        "9F27": the9F27,
        "9F34": the9F34,
        "9F1A": the9F1A,
        "9F37": the9F37,
        "9F1E": the9F1E,
        "9F21": the9F21,
        "9F41": the9F41,
        'strTrack2': strTrack2,
        'pan': pan,
        'expiry': expiry,
        'src': src,
        'CardPin': cardPin,
        'ksn': ksn
    ,
      };
}
