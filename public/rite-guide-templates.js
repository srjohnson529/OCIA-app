// Based on user-provided Rites.pdf. Spanish is a translation for preparation, not an official ritual edition.
const RiteGuideTemplates = [
  {
    "id": "acceptance",
    "en": {
      "title": "The Rite of Acceptance into the Order of Catechumens",
      "meaning": "The Rite of Acceptance is the first major public step for an unbaptized person preparing to  become Catholic. During this rite, the Church formally accepts you as a catechumen—someone  preparing to receive Baptism, Confirmation, and the Eucharist. You publicly express your desire  to follow Christ, and the Church promises to support you through prayer, instruction, and  fellowship.",
      "context": "This rite marks the transition from the Period of Evangelization and Precatechumenate into the  Period of the Catechumenate. The catechumenate is an extended period of spiritual formation  during which you will grow in faith, participate more deeply in the life of the Church, and  prepare for the Sacraments of Christian Initiation.",
      "studentActions": "You will gather with the other inquirers, your sponsor, and members of the parish community.  You will be asked what you seek from the Church and what faith offers you. You will publicly  express your intention to follow Christ and continue your formation. The celebrant will trace the  Sign of the Cross upon your forehead, and your sponsor may sign your senses with the cross.  You will then be invited to enter the church and listen to the Word of God.",
      "ministerActions": "The celebrant may ask your name and questions such as:\n• “What do you ask of God’s Church?”\n• “What does faith offer you?”\n• “Are you ready to begin this journey under the guidance of Christ?”\n• “Are you prepared to listen to the apostles’ teaching, gather with the Christian  community, and join us in prayer and service?” The exact wording and some elements of the celebration may vary according to the approved  ritual options and local parish practice.",
      "preparation": "Pray for openness, courage, and a sincere desire to follow Christ. Reflect on why you wish to  become Catholic and what you are seeking from God and the Church. Review the responses with  your OCIA instructor, arrive early, and follow any instructions concerning seating, sponsors,  clothing, or rehearsal. Your parish will provide the date, time, location, and any additional details  needed for the celebration.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Rito de aceptación en el orden de los catecúmenos",
      "meaning": "El Rito de aceptación es el primer gran paso público de una persona no bautizada que se prepara para hacerse católica. En este rito, la Iglesia te acepta formalmente como catecúmeno: alguien que se prepara para recibir el Bautismo, la Confirmación y la Eucaristía. Expresas públicamente tu deseo de seguir a Cristo y la Iglesia promete apoyarte mediante la oración, la enseñanza y la comunidad.",
      "context": "Este rito marca el paso del período de evangelización y precatecumenado al período del catecumenado. El catecumenado es un tiempo prolongado de formación espiritual en el que crecerás en la fe, participarás más profundamente en la vida de la Iglesia y te prepararás para los sacramentos de la iniciación cristiana.",
      "studentActions": "Te reunirás con los demás interesados, tu acompañante y miembros de la comunidad parroquial. Se te preguntará qué buscas en la Iglesia y qué te ofrece la fe. Expresarás públicamente tu intención de seguir a Cristo y continuar tu formación. El celebrante trazará la señal de la cruz en tu frente, y tu acompañante podrá signar tus sentidos con la cruz. Después se te invitará a entrar en la iglesia y escuchar la Palabra de Dios.",
      "ministerActions": "El celebrante puede preguntarte tu nombre y hacer preguntas como:\n• «¿Qué pides a la Iglesia de Dios?»\n• «¿Qué te ofrece la fe?»\n• «¿Estás dispuesto a comenzar este camino bajo la guía de Cristo?»\n• «¿Estás dispuesto a escuchar la enseñanza de los apóstoles, reunirte con la comunidad cristiana y unirte a nosotros en la oración y el servicio?»\nLa formulación exacta y algunos elementos pueden variar según las opciones del ritual aprobado y la práctica parroquial.",
      "preparation": "Reza para tener apertura, valentía y un deseo sincero de seguir a Cristo. Reflexiona sobre por qué deseas hacerte católico y qué buscas en Dios y en la Iglesia. Repasa las respuestas con tu instructor de OCIA, llega temprano y sigue las indicaciones sobre asientos, acompañantes, vestimenta o ensayo. Tu parroquia proporcionará la fecha, hora, lugar y demás detalles.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "welcoming",
    "en": {
      "title": "The Rite of Welcoming the Candidates",
      "meaning": "The Rite of Welcoming is a public celebration for people who have already been validly baptized  and are preparing to enter the full communion of the Catholic Church or complete their Christian  initiation. During this rite, the Church welcomes you as a candidate and recognizes the Christian  faith and baptism you have already received.",
      "context": "This rite normally marks a new stage of formation for baptized candidates. Unlike catechumens,  candidates are already Christians through Baptism and are not preparing to be baptized again.  Instead, they continue growing in Catholic faith and practice as they prepare for Reception into  Full Communion, Confirmation, and the Eucharist—or for Confirmation and the Eucharist if they  were baptized Catholic.",
      "studentActions": "You will gather with the other candidates, your sponsor, and the parish community. You will  publicly express your desire to continue following Christ within the Catholic Church. The  celebrant will ask about your intentions and may trace the Sign of the Cross upon your forehead.  Your sponsor may also sign your senses with the cross. You will then be invited into the church  to listen to the Word of God and join the community in prayer.",
      "ministerActions": "The celebrant may ask your name and questions such as:\n• “What do you ask of God’s Church?”\n• “What does faith offer you?”\n• “Are you ready to listen to the apostles’ teaching, gather with us in prayer, and share in  the life and mission of the Church?”\n• “Are you prepared to continue your journey of faith under the guidance of Christ?” The wording and some elements may vary according to the approved ritual options and local  parish practice.",
      "preparation": "Pray for deeper faith and openness to the guidance of the Holy Spirit. Reflect upon your Baptism,  your relationship with Christ, and why you desire to enter full communion with the Catholic  Church or complete your initiation. Review the responses with your OCIA instructor, speak with  your sponsor, arrive early, and follow any parish instructions concerning rehearsal, seating, or  clothing. Your parish will provide the date, time, location, and other details for the celebration.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Rito de acogida de los candidatos",
      "meaning": "El Rito de acogida es una celebración pública para personas válidamente bautizadas que se preparan para entrar en la plena comunión de la Iglesia católica o completar su iniciación cristiana. La Iglesia te acoge como candidato y reconoce la fe cristiana y el Bautismo que ya has recibido.",
      "context": "Este rito normalmente marca una nueva etapa de formación para los candidatos bautizados. A diferencia de los catecúmenos, ya son cristianos por el Bautismo y no se preparan para ser bautizados otra vez. Continúan creciendo en la fe y la práctica católicas mientras se preparan para la recepción en la plena comunión, la Confirmación y la Eucaristía, o para la Confirmación y la Eucaristía si fueron bautizados católicos.",
      "studentActions": "Te reunirás con los demás candidatos, tu acompañante y la comunidad parroquial. Expresarás públicamente tu deseo de seguir a Cristo dentro de la Iglesia católica. El celebrante preguntará por tus intenciones y podrá trazar la señal de la cruz en tu frente. Tu acompañante también podrá signar tus sentidos. Después se te invitará a entrar en la iglesia para escuchar la Palabra de Dios y orar con la comunidad.",
      "ministerActions": "El celebrante puede preguntarte tu nombre y hacer preguntas como:\n• «¿Qué pides a la Iglesia de Dios?»\n• «¿Qué te ofrece la fe?»\n• «¿Estás dispuesto a escuchar la enseñanza de los apóstoles, reunirte con nosotros en oración y compartir la vida y la misión de la Iglesia?»\n• «¿Estás dispuesto a continuar tu camino de fe bajo la guía de Cristo?»\nLa formulación y algunos elementos pueden variar según las opciones del ritual aprobado y la práctica parroquial.",
      "preparation": "Reza por una fe más profunda y apertura a la guía del Espíritu Santo. Reflexiona sobre tu Bautismo, tu relación con Cristo y tu deseo de entrar en la plena comunión católica o completar tu iniciación. Repasa las respuestas con tu instructor, habla con tu acompañante, llega temprano y sigue las indicaciones sobre ensayo, asientos o vestimenta. La parroquia proporcionará la fecha, hora, lugar y demás detalles.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "election",
    "en": {
      "title": "The Rite of Election",
      "meaning": "The Rite of Election is the celebration in which the Church, through the bishop, formally chooses  the catechumens who are ready to receive the Sacraments of Christian Initiation. From this point  forward, they are called the “elect.” This election expresses God’s initiative: the Church  recognizes that God has called them and that they have responded in faith.",
      "context": "The Rite of Election normally takes place at the cathedral on or near the First Sunday of Lent. It  concludes the Period of the Catechumenate and begins the Period of Purification and  Enlightenment—the final and more intensely spiritual preparation for Baptism, Confirmation,  and the Eucharist at Easter. This rite is specifically for unbaptized catechumens. Baptized candidates may participate in the  separate Call to Continuing Conversion, which is sometimes celebrated at the same diocesan  gathering.",
      "studentActions": "You will be presented to the bishop with your godparent and the other catechumens. Your  godparent will affirm your readiness for initiation. You may be asked to declare your desire to  receive the sacraments, and your name will be presented in the Book of the Elect. After the  bishop declares the Church’s election, you will be recognized as one of the elect and enter your  final period of preparation for Easter.",
      "ministerActions": "The bishop or another celebrant may ask your godparent whether you have:\n• Faithfully listened to God’s Word.\n• Responded to that Word and begun to live in God’s presence.\n• Participated in the life and prayer of the Christian community.\n• Shown evidence of genuine conversion and readiness for the sacraments. The bishop may then ask whether you wish to enter fully into the life of the Church through  Baptism, Confirmation, and the Eucharist. He will formally declare you to be a member of the  elect and encourage you to remain faithful during your Lenten preparation.",
      "preparation": "Pray for humility, perseverance, and openness to the grace of God. Reflect upon your conversion,  your desire for the sacraments, and your commitment to follow Christ within his Church. Speak  with your godparent and follow all parish instructions regarding transportation, arrival time,  seating, attire, and the Book of the Elect. Your parish will provide the date, time, location, and  diocesan details for the celebration.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Rito de elección",
      "meaning": "El Rito de elección es la celebración en la que la Iglesia, por medio del obispo, elige formalmente a los catecúmenos preparados para recibir los sacramentos de la iniciación cristiana. Desde ese momento se llaman «elegidos». Esta elección expresa la iniciativa de Dios: la Iglesia reconoce que Dios los ha llamado y que han respondido con fe.",
      "context": "Normalmente se celebra en la catedral el primer domingo de Cuaresma o cerca de esa fecha. Concluye el catecumenado e inicia el período de purificación e iluminación: la preparación final y más intensamente espiritual para el Bautismo, la Confirmación y la Eucaristía en Pascua.\nEste rito es específicamente para catecúmenos no bautizados. Los candidatos bautizados pueden participar en el distinto Llamado a la conversión continua, que a veces se celebra en el mismo encuentro diocesano.",
      "studentActions": "Serás presentado al obispo junto con tu padrino o madrina y los demás catecúmenos. Tu padrino o madrina afirmará tu preparación para la iniciación. Podrán pedirte que declares tu deseo de recibir los sacramentos, y tu nombre se presentará en el Libro de los Elegidos. Después de que el obispo declare la elección de la Iglesia, serás reconocido como uno de los elegidos y comenzarás tu preparación final para Pascua.",
      "ministerActions": "El obispo u otro celebrante puede preguntar a tu padrino o madrina si has:\n• Escuchado fielmente la Palabra de Dios.\n• Respondido a esa Palabra y comenzado a vivir en la presencia de Dios.\n• Participado en la vida y la oración de la comunidad cristiana.\n• Mostrado una conversión auténtica y preparación para los sacramentos.\nEl obispo puede preguntar si deseas entrar plenamente en la vida de la Iglesia mediante el Bautismo, la Confirmación y la Eucaristía. Te declarará formalmente miembro de los elegidos y te animará a perseverar durante la Cuaresma.",
      "preparation": "Reza por humildad, perseverancia y apertura a la gracia de Dios. Reflexiona sobre tu conversión, tu deseo de recibir los sacramentos y tu compromiso de seguir a Cristo en su Iglesia. Habla con tu padrino o madrina y sigue las instrucciones sobre transporte, llegada, asientos, vestimenta y el Libro de los Elegidos. La parroquia proporcionará la fecha, hora, lugar y detalles diocesanos.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "conversion",
    "en": {
      "title": "The Call to Continuing Conversion",
      "meaning": "The Call to Continuing Conversion is the celebration in which the Church recognizes the  spiritual progress of baptized candidates and invites them to continue their conversion as they  prepare to complete their Christian initiation or enter the full communion of the Catholic Church.  It honors the Baptism they have already received while calling them to deeper faith, repentance,  and discipleship.",
      "context": "This celebration normally takes place at the beginning of Lent, often at the cathedral with the  bishop. It may be celebrated together with the Rite of Election for catechumens, but the two rites  remain distinct. Catechumens are elected for Baptism, while baptized candidates are called to  continuing conversion and preparation for Confirmation and the Eucharist or Reception into Full  Communion.",
      "studentActions": "You will be presented to the bishop or celebrant with your sponsor and the other candidates. Your  sponsor will affirm that you have listened to God’s Word, deepened your relationship with  Christ, participated in the Church’s life, and shown readiness to continue toward the sacraments.  You may then be asked to affirm your intention to continue your conversion and complete your  preparation.",
      "ministerActions": "The bishop or celebrant may ask your sponsor whether you have:\n• Grown in your understanding of the Catholic faith.\n• Deepened your life of prayer and relationship with Christ.\n• Participated in the worship and life of the Christian community.\n• Demonstrated a sincere desire to live as a faithful Catholic.\n• Shown readiness to complete your sacramental preparation. The celebrant may then ask whether you intend to continue your conversion and prepare  faithfully for the sacraments. The Church will affirm and pray for you as you enter the final stage  of preparation.",
      "preparation": "Reflect on how Christ has been working in your life and where you are still being called to  conversion. Continue developing habits of prayer, participation at Mass, study, and works of  charity. Speak with your sponsor, since your sponsor may be asked to affirm your readiness  publicly. Your parish will provide instructions regarding transportation, arrival time, seating,  attire, and the date and location of the diocesan celebration.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Llamado a la conversión continua",
      "meaning": "En esta celebración, la Iglesia reconoce el progreso espiritual de los candidatos bautizados y los invita a continuar su conversión mientras se preparan para completar su iniciación o entrar en la plena comunión de la Iglesia católica. Honra el Bautismo ya recibido y llama a una fe, un arrepentimiento y un discipulado más profundos.",
      "context": "Normalmente se celebra al comienzo de la Cuaresma, a menudo en la catedral con el obispo. Puede celebrarse junto con el Rito de elección, pero los dos ritos siguen siendo distintos. Los catecúmenos son elegidos para el Bautismo; los candidatos bautizados son llamados a la conversión continua y a prepararse para la Confirmación y la Eucaristía o la recepción en la plena comunión.",
      "studentActions": "Serás presentado al obispo o celebrante con tu acompañante y los demás candidatos. Tu acompañante afirmará que has escuchado la Palabra de Dios, profundizado tu relación con Cristo, participado en la vida de la Iglesia y mostrado preparación para avanzar hacia los sacramentos. Podrán pedirte que confirmes tu intención de continuar la conversión y completar tu preparación.",
      "ministerActions": "El obispo o celebrante puede preguntar a tu acompañante si has:\n• Crecido en la comprensión de la fe católica.\n• Profundizado tu oración y tu relación con Cristo.\n• Participado en el culto y la vida de la comunidad cristiana.\n• Mostrado un deseo sincero de vivir como católico fiel.\n• Mostrado preparación para completar tu formación sacramental.\nDespués puede preguntarte si deseas continuar tu conversión y prepararte fielmente para los sacramentos. La Iglesia te apoyará y rezará por ti al comenzar esta etapa final.",
      "preparation": "Reflexiona sobre cómo Cristo ha actuado en tu vida y dónde sigues siendo llamado a la conversión. Continúa cultivando la oración, la participación en la Misa, el estudio y las obras de caridad. Habla con tu acompañante, que podrá ser invitado a afirmar públicamente tu preparación. La parroquia dará instrucciones sobre transporte, llegada, asientos, vestimenta, fecha y lugar de la celebración.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "scrutiny-2",
    "en": {
      "title": "The Second Scrutiny",
      "meaning": "The Second Scrutiny is a prayerful rite of self-examination, repentance, and spiritual  strengthening for the elect. Through prayers of intercession and exorcism, the Church asks Christ  to reveal and heal the spiritual blindness caused by sin. It is not a test or public confession of  personal sins but an opportunity to receive greater freedom, clarity, and faith.",
      "context": "The Second Scrutiny is celebrated on the Fourth Sunday of Lent during the Period of Purification  and Enlightenment. It is intended specifically for the elect—the unbaptized preparing to receive  Baptism, Confirmation, and the Eucharist at Easter. Its Gospel is the account of Jesus healing the  man born blind, which reveals Christ as the Light of the World.",
      "studentActions": "After listening to the Gospel and homily, you will come forward with the other members of the  elect. You may be invited to bow or kneel while the community prays for you. Your godparent  may place a hand on your shoulder. You will silently examine your life, ask Christ to heal your  spiritual blindness, and pray for the grace to recognize and follow him more faithfully.",
      "ministerActions": "The celebrant will invite the elect and the assembly to pray. The prayers may ask that you:\n• Be freed from falsehood, confusion, and spiritual blindness.\n• Recognize Christ as the Light of the World.\n• See sin clearly and turn away from it.\n• Grow in faith, courage, and holiness.\n• Become a witness to the truth of the Gospel.\n• Be strengthened for Baptism and life in Christ. The celebrant will then offer a prayer of exorcism, asking Christ to free, heal, protect, and  enlighten you. This is a prayer for deliverance from sin and evil, not a solemn exorcism  associated with demonic possession.",
      "preparation": "Prepare through prayer, fasting, examination of conscience, and reflection upon the healing of  the man born blind in John 9. Consider where sin, fear, pride, or misunderstanding may prevent  you from seeing Christ clearly. Ask God to deepen your faith and help you live as a child of  light. Follow parish instructions regarding arrival time, seating, godparents, rehearsal, and  dismissal after the Liturgy of the Word.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Segundo escrutinio",
      "meaning": "El Segundo escrutinio es un rito de examen interior, arrepentimiento y fortalecimiento espiritual para los elegidos. Mediante intercesiones y oraciones de exorcismo, la Iglesia pide a Cristo que revele y sane la ceguera espiritual causada por el pecado. No es una prueba ni una confesión pública de pecados personales, sino una ocasión para recibir mayor libertad, claridad y fe.",
      "context": "Se celebra el cuarto domingo de Cuaresma durante el período de purificación e iluminación. Está destinado a los elegidos: los no bautizados que se preparan para recibir el Bautismo, la Confirmación y la Eucaristía en Pascua. Su Evangelio relata la curación del ciego de nacimiento y revela a Cristo como la Luz del Mundo.",
      "studentActions": "Después del Evangelio y la homilía, pasarás al frente con los demás elegidos. Podrán invitarte a inclinarte o arrodillarte mientras la comunidad reza por ti. Tu padrino o madrina podrá poner una mano sobre tu hombro. En silencio examinarás tu vida, pedirás a Cristo que sane tu ceguera espiritual y rezarás por la gracia de reconocerlo y seguirlo más fielmente.",
      "ministerActions": "El celebrante invitará a los elegidos y a la asamblea a orar. Las oraciones pueden pedir que:\n• Seas liberado de la falsedad, la confusión y la ceguera espiritual.\n• Reconozcas a Cristo como la Luz del Mundo.\n• Veas claramente el pecado y te apartes de él.\n• Crezcas en fe, valentía y santidad.\n• Seas testigo de la verdad del Evangelio.\n• Seas fortalecido para el Bautismo y la vida en Cristo.\nEl celebrante ofrecerá una oración de exorcismo para que Cristo te libere, sane, proteja e ilumine. Es una oración de liberación del pecado y del mal, no un exorcismo solemne relacionado con una posesión demoníaca.",
      "preparation": "Prepárate con oración, ayuno, examen de conciencia y reflexión sobre la curación del ciego de nacimiento en Juan 9. Considera cómo el pecado, el miedo, el orgullo o la incomprensión pueden impedirte ver claramente a Cristo. Pide a Dios que profundice tu fe y te ayude a vivir como hijo de la luz. Sigue las instrucciones sobre llegada, asientos, padrinos, ensayo y despedida después de la Liturgia de la Palabra.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "scrutiny-3",
    "en": {
      "title": "The Third Scrutiny",
      "meaning": "The Third Scrutiny is the final prayerful rite of self-examination, repentance, and spiritual  strengthening for the elect. Through prayers of intercession and exorcism, the Church asks Christ  to free them from the power of sin and death and prepare them to receive new life in Baptism. It  is not a test or public confession of personal sins.",
      "context": "The Third Scrutiny is celebrated on the Fifth Sunday of Lent during the Period of Purification  and Enlightenment. It is intended specifically for the elect—the unbaptized preparing to receive  Baptism, Confirmation, and the Eucharist at Easter. Its Gospel is the raising of Lazarus, which  reveals Christ as the Resurrection and the Life.",
      "studentActions": "After listening to the Gospel and homily, you will come forward with the other members of the  elect. You may be invited to bow or kneel while the community prays for you. Your godparent  may place a hand on your shoulder. You will silently examine your life, entrust your fears and  weaknesses to Christ, and ask him to free you from sin and lead you into the new life of Baptism.",
      "ministerActions": "The celebrant will invite the elect and the assembly to pray. The prayers may ask that you:\n• Be freed from the power of sin and spiritual death.\n• Place your hope in Christ, the Resurrection and the Life.\n• Be strengthened against fear, discouragement, and temptation.\n• Die to sin and rise to new life with Christ.\n• Bear witness to the life-giving power of the Gospel.\n• Be prepared to receive Baptism, Confirmation, and the Eucharist. The celebrant will then offer a prayer of exorcism, asking Christ to free, protect, and strengthen  you. This is a prayer for deliverance from sin and evil, not a solemn exorcism associated with  demonic possession.",
      "preparation": "Prepare through prayer, fasting, examination of conscience, and reflection upon the raising of  Lazarus in John 11. Consider what must be surrendered or brought to new life within you.  Reflect on Christ’s promise of resurrection and ask for the grace to die to sin and live fully in  him. Follow parish instructions regarding arrival time, seating, godparents, rehearsal, and  dismissal after the Liturgy of the Word.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Tercer escrutinio",
      "meaning": "El Tercer escrutinio es el último rito de examen interior, arrepentimiento y fortalecimiento espiritual para los elegidos. Mediante intercesiones y oraciones de exorcismo, la Iglesia pide a Cristo que los libere del poder del pecado y de la muerte y los prepare para la nueva vida del Bautismo. No es una prueba ni una confesión pública de pecados personales.",
      "context": "Se celebra el quinto domingo de Cuaresma durante el período de purificación e iluminación. Está destinado a los elegidos: los no bautizados que se preparan para el Bautismo, la Confirmación y la Eucaristía en Pascua. Su Evangelio es la resurrección de Lázaro, que revela a Cristo como la Resurrección y la Vida.",
      "studentActions": "Después del Evangelio y la homilía, pasarás al frente con los demás elegidos. Podrán invitarte a inclinarte o arrodillarte mientras la comunidad reza por ti. Tu padrino o madrina podrá poner una mano sobre tu hombro. Examinarás tu vida en silencio, confiarás a Cristo tus temores y debilidades y le pedirás que te libere del pecado y te conduzca a la nueva vida del Bautismo.",
      "ministerActions": "El celebrante invitará a los elegidos y a la asamblea a orar. Las oraciones pueden pedir que:\n• Seas liberado del poder del pecado y de la muerte espiritual.\n• Pongas tu esperanza en Cristo, la Resurrección y la Vida.\n• Seas fortalecido frente al miedo, el desaliento y la tentación.\n• Mueras al pecado y resucites a una vida nueva con Cristo.\n• Des testimonio del poder vivificador del Evangelio.\n• Te prepares para recibir el Bautismo, la Confirmación y la Eucaristía.\nEl celebrante ofrecerá una oración de exorcismo para que Cristo te libere, proteja y fortalezca. Es una oración de liberación del pecado y del mal, no un exorcismo solemne relacionado con una posesión demoníaca.",
      "preparation": "Prepárate con oración, ayuno, examen de conciencia y reflexión sobre la resurrección de Lázaro en Juan 11. Considera qué necesitas entregar a Dios o qué necesita una vida nueva en ti. Reflexiona sobre la promesa de resurrección de Cristo y pide la gracia de morir al pecado y vivir plenamente en él. Sigue las instrucciones sobre llegada, asientos, padrinos, ensayo y despedida después de la Liturgia de la Palabra.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "lords-prayer",
    "en": {
      "title": "The Presentation of the Lord’s Prayer",
      "meaning": "The Presentation, or “Handing On,” of the Lord’s Prayer is the rite in which the Church entrusts  the Our Father to the elect. This ancient prayer was given by Jesus to his disciples and expresses  the faith, hope, and desires of those who have become children of God. The elect receive it in  preparation for the new life and divine adoption given through Baptism.",
      "context": "The Presentation of the Lord’s Prayer is celebrated during the Period of Purification and  Enlightenment, ordinarily during the fifth week of Lent after the Third Scrutiny, though it may  occur at another suitable time. It is intended for the elect—the unbaptized preparing to receive  the Sacraments of Christian Initiation.",
      "studentActions": "You will come forward with the other members of the elect and listen as the Gospel account of  Jesus teaching the Lord’s Prayer is proclaimed. The Church symbolically entrusts this prayer to  you so that it may become the pattern of your relationship with God. You will listen prayerfully,  reflect upon its petitions, and prepare to pray it fully as one of God’s children after your Baptism.",
      "ministerActions": "The celebrant will invite you to hear the prayer that Christ gave to his disciples. After the Gospel  is proclaimed, he may offer instruction on the meaning of the Lord’s Prayer and pray that you  will:\n• Come to know God as your Father.\n• Seek the holiness of God’s name and the coming of his Kingdom.\n• Trust God for your daily needs.\n• Receive and extend forgiveness.\n• Remain faithful during temptation.\n• Be delivered from evil.\n• Live according to the dignity of God’s adopted children. The rite concludes with prayer over the elect, asking that they may soon receive new life through  Baptism.",
      "preparation": "Prayerfully reflect upon each petition of the Our Father in Matthew 6:9–13. Consider what the  prayer teaches about God, forgiveness, trust, temptation, and Christian discipleship. Ask God to  prepare you to live as his adopted child and make the Lord’s Prayer the pattern of your spiritual  life. Follow parish instructions regarding the date, arrival time, seating, and any rehearsal.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Entrega de la Oración del Señor",
      "meaning": "La presentación o entrega de la Oración del Señor es el rito en el que la Iglesia confía el Padrenuestro a los elegidos. Jesús dio esta antigua oración a sus discípulos; expresa la fe, la esperanza y los deseos de quienes han llegado a ser hijos de Dios. Los elegidos la reciben como preparación para la vida nueva y la adopción divina dadas mediante el Bautismo.",
      "context": "Se celebra durante el período de purificación e iluminación, normalmente en la quinta semana de Cuaresma después del Tercer escrutinio, aunque puede hacerse en otro momento oportuno. Está destinada a los elegidos: los no bautizados que se preparan para los sacramentos de la iniciación cristiana.",
      "studentActions": "Pasarás al frente con los demás elegidos y escucharás el Evangelio en el que Jesús enseña la Oración del Señor. La Iglesia te confía simbólicamente esta oración para que sea el modelo de tu relación con Dios. La escucharás en oración, reflexionarás sobre sus peticiones y te prepararás para rezarla plenamente como hijo de Dios después del Bautismo.",
      "ministerActions": "El celebrante te invitará a escuchar la oración que Cristo dio a sus discípulos. Después del Evangelio, podrá explicar su significado y rezar para que:\n• Conozcas a Dios como tu Padre.\n• Busques la santidad de su nombre y la llegada de su Reino.\n• Confíes a Dios tus necesidades diarias.\n• Recibas y concedas el perdón.\n• Permanezcas fiel en la tentación.\n• Seas liberado del mal.\n• Vivas según la dignidad de los hijos adoptivos de Dios.\nEl rito termina con una oración sobre los elegidos, pidiendo que reciban pronto la vida nueva mediante el Bautismo.",
      "preparation": "Reflexiona en oración sobre cada petición del Padrenuestro en Mateo 6,9–13. Considera lo que enseña acerca de Dios, el perdón, la confianza, la tentación y el discipulado cristiano. Pide a Dios que te prepare para vivir como hijo suyo y que esta oración sea el modelo de tu vida espiritual. Sigue las instrucciones sobre fecha, llegada, asientos y ensayo.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "creed",
    "en": {
      "title": "The Presentation of the Creed",
      "meaning": "The Presentation, or “Handing On,” of the Creed is the rite in which the Church entrusts the  summary of the Catholic faith to the elect. The Creed proclaims the saving works of God— Father, Son, and Holy Spirit—and expresses the faith into which the elect will soon be baptized.  Receiving the Creed represents receiving the faith of the Church and accepting the responsibility  to profess and live it faithfully.",
      "context": "The Presentation of the Creed is celebrated during the Period of Purification and Enlightenment,  ordinarily during the week following the First Scrutiny, though it may take place at another  suitable time. It is intended for the elect—the unbaptized preparing to receive Baptism,  Confirmation, and the Eucharist.",
      "studentActions": "You will come forward with the other members of the elect and listen while the assembly  solemnly professes the Creed. The Church symbolically entrusts this faith to you as a spiritual  treasure. You will receive it attentively, reflect upon its meaning, and prepare to profess the  Church’s faith as your own at Baptism.",
      "ministerActions": "The celebrant will invite you to listen carefully as the Church proclaims the faith by which you  will be justified and in which you will receive Baptism. He may then pray that you will:\n• Come to know and profess the one true God.\n• Believe firmly in the Father, Son, and Holy Spirit.\n• Understand more deeply the saving work of Jesus Christ.\n• Preserve the faith faithfully throughout your life.\n• Reject what is contrary to the Gospel.\n• Profess the Church’s faith with both your words and your actions. The celebrant may also offer a brief explanation of the Creed and pray over you as you prepare  for the sacraments.",
      "preparation": "Prayerfully read and reflect upon the Apostles’ Creed or the Nicene Creed, according to the form  your parish will use. Consider each statement of faith and bring any questions to your OCIA  instructor. Ask God to strengthen your understanding and help you live the faith you will profess.  Follow parish instructions regarding the date, arrival time, seating, and any rehearsal.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Entrega del Credo",
      "meaning": "La presentación o entrega del Credo es el rito en el que la Iglesia confía a los elegidos el resumen de la fe católica. El Credo proclama las obras salvadoras de Dios —Padre, Hijo y Espíritu Santo— y expresa la fe en la que pronto serán bautizados. Recibirlo representa recibir la fe de la Iglesia y aceptar la responsabilidad de profesarla y vivirla fielmente.",
      "context": "Se celebra durante el período de purificación e iluminación, normalmente en la semana posterior al Primer escrutinio, aunque puede celebrarse en otro momento oportuno. Está destinada a los elegidos: los no bautizados que se preparan para el Bautismo, la Confirmación y la Eucaristía.",
      "studentActions": "Pasarás al frente con los demás elegidos y escucharás a la asamblea profesar solemnemente el Credo. La Iglesia te confía simbólicamente esta fe como un tesoro espiritual. La recibirás con atención, reflexionarás sobre su significado y te prepararás para profesar como propia la fe de la Iglesia en el Bautismo.",
      "ministerActions": "El celebrante te invitará a escuchar atentamente la fe que proclama la Iglesia, por la que serás justificado y en la que recibirás el Bautismo. Podrá rezar para que:\n• Conozcas y profeses al único Dios verdadero.\n• Creas firmemente en el Padre, el Hijo y el Espíritu Santo.\n• Comprendas más profundamente la obra salvadora de Jesucristo.\n• Conserves fielmente la fe durante toda tu vida.\n• Rechaces lo contrario al Evangelio.\n• Profeses la fe de la Iglesia con palabras y acciones.\nTambién podrá explicar brevemente el Credo y orar sobre ti mientras te preparas para los sacramentos.",
      "preparation": "Lee y medita en oración el Credo de los Apóstoles o el Credo Niceno, según el que use tu parroquia. Considera cada afirmación de fe y comparte tus preguntas con tu instructor de OCIA. Pide a Dios que fortalezca tu comprensión y te ayude a vivir la fe que profesarás. Sigue las instrucciones sobre fecha, llegada, asientos y ensayo.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "initiation",
    "en": {
      "title": "The Sacraments of Christian Initiation",
      "meaning": "Baptism, Confirmation, and the Eucharist are the three Sacraments of Christian Initiation.  Through Baptism, you are freed from sin, reborn as a child of God, and incorporated into Christ  and his Church. Through Confirmation, you are sealed and strengthened by the Holy Spirit. In  the Eucharist, you receive the Body and Blood of Christ and enter fully into the Church’s  sacramental life.",
      "context": "For the elect, the Sacraments of Christian Initiation are ordinarily celebrated together during the  Easter Vigil. This celebration concludes the Period of Purification and Enlightenment and begins  the Period of Mystagogy—a time for reflecting upon the sacraments received and growing more  deeply in the Christian life. Baptized candidates are not baptized again. Depending upon their circumstances, they may make  a Profession of Faith, be received into the full communion of the Catholic Church, receive  Confirmation, and receive the Eucharist.",
      "studentActions": "You will come forward with your godparent, renounce sin, and profess the faith of the Church.  You will then be baptized with water in the name of the Father, the Son, and the Holy Spirit.  After Baptism, you will receive a white garment and a baptismal candle as signs of your new life  in Christ. You will then be anointed with Sacred Chrism as the celebrant confirms you with the Gift of the  Holy Spirit. Later in the Mass, you will approach the altar and receive the Body and Blood of  Christ in Holy Communion for the first time.",
      "ministerActions": "Before Baptism, the celebrant will ask you to renounce sin and profess your faith. The questions  will concern your rejection of sin and your belief in:\n• God the Father, creator of heaven and earth.\n• Jesus Christ, his Son, who died and rose again.\n• The Holy Spirit.\n• The holy Catholic Church.\n• The communion of saints.\n• The forgiveness of sins.\n• The resurrection of the body and eternal life. The celebrant will baptize you by pouring or immersing you in water three times while invoking  the Father, Son, and Holy Spirit. During Confirmation, he will anoint your forehead with Sacred  Chrism and say, “Be sealed with the Gift of the Holy Spirit.” You will respond, “Amen.”",
      "preparation": "Prepare through prayer, fasting, examination of conscience, and reflection upon the sacraments  you are about to receive. Review the baptismal promises and practical instructions with your  OCIA team. Confirm your baptismal name, godparent, clothing, arrival time, seating, and  rehearsal details. Your parish will also explain what to bring, what clothing to wear for Baptism,  and how to receive Confirmation and Holy Communion.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Sacramentos de la iniciación cristiana",
      "meaning": "El Bautismo, la Confirmación y la Eucaristía son los tres sacramentos de la iniciación cristiana. Por el Bautismo eres liberado del pecado, renaces como hijo de Dios y eres incorporado a Cristo y a su Iglesia. Por la Confirmación eres sellado y fortalecido por el Espíritu Santo. En la Eucaristía recibes el Cuerpo y la Sangre de Cristo y entras plenamente en la vida sacramental de la Iglesia.",
      "context": "Para los elegidos, estos sacramentos normalmente se celebran juntos en la Vigilia Pascual. La celebración concluye el período de purificación e iluminación y comienza la mistagogía: un tiempo para reflexionar sobre los sacramentos recibidos y profundizar en la vida cristiana.\nLos candidatos bautizados no vuelven a bautizarse. Según sus circunstancias, pueden hacer una profesión de fe, ser recibidos en la plena comunión católica y recibir la Confirmación y la Eucaristía.",
      "studentActions": "Pasarás al frente con tu padrino o madrina, renunciarás al pecado y profesarás la fe de la Iglesia. Serás bautizado con agua en el nombre del Padre, del Hijo y del Espíritu Santo. Recibirás una vestidura blanca y una vela bautismal como signos de tu nueva vida en Cristo.\nDespués serás ungido con el Santo Crisma cuando el celebrante te confirme con el don del Espíritu Santo. Más adelante en la Misa te acercarás al altar y recibirás por primera vez el Cuerpo y la Sangre de Cristo en la Comunión.",
      "ministerActions": "Antes del Bautismo, el celebrante te pedirá renunciar al pecado y profesar tu fe. Las preguntas tratarán de tu rechazo del pecado y tu fe en:\n• Dios Padre, creador del cielo y de la tierra.\n• Jesucristo, su Hijo, que murió y resucitó.\n• El Espíritu Santo.\n• La santa Iglesia católica.\n• La comunión de los santos.\n• El perdón de los pecados.\n• La resurrección de los muertos y la vida eterna.\nTe bautizará derramando agua o sumergiéndote tres veces mientras invoca al Padre, al Hijo y al Espíritu Santo. En la Confirmación ungirá tu frente con el Santo Crisma y dirá: «Recibe por esta señal el don del Espíritu Santo». Responderás: «Amén».",
      "preparation": "Prepárate con oración, ayuno, examen de conciencia y reflexión sobre los sacramentos que recibirás. Repasa las promesas bautismales y las instrucciones prácticas con tu equipo de OCIA. Confirma el nombre bautismal, padrino o madrina, vestimenta, llegada, asientos y ensayo. La parroquia explicará qué llevar, qué ropa usar para el Bautismo y cómo recibir la Confirmación y la Comunión.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "combined-entrance",
    "en": {
      "title": "The Combined Rite of Acceptance and Welcoming",
      "meaning": "This celebration combines the Rite of Acceptance into the Order of Catechumens for the  unbaptized with the Rite of Welcoming for baptized candidates. Although the two groups  celebrate together, the Church carefully preserves their different sacramental situations. The  unbaptized are formally accepted as catechumens, while baptized Christians are welcomed as  candidates preparing to complete their initiation or enter the full communion of the Catholic  Church.",
      "context": "The combined rite normally marks the transition from the initial period of inquiry into a more  intentional period of formation. Catechumens enter the Period of the Catechumenate and begin  preparing for Baptism, Confirmation, and the Eucharist. Candidates continue developing their  Catholic faith as they prepare for Reception into Full Communion or the completion of their  Christian initiation.",
      "studentActions": "You will gather with the other participants, your sponsor, and the parish community, usually near  the entrance of the church. You may be asked your name, what you seek from the Church, and  whether you are prepared to continue following Christ. The celebrant will trace the Sign of the Cross upon your forehead, and your sponsor may sign  your other senses. You will then be invited into the church to listen to God’s Word and join the  community in prayer. Throughout the rite, some prayers and questions will be directed  specifically to catechumens and others to baptized candidates.",
      "ministerActions": "The celebrant may ask questions such as:\n• “What do you ask of God’s Church?”\n• “What does faith offer you?”\n• “Are you ready to begin or continue this journey under the guidance of Christ?”\n• “Are you prepared to listen to the apostles’ teaching, gather with the Christian  community, and join us in prayer and service?” He may also ask the sponsors and assembly whether they are prepared to support the  catechumens and candidates. The wording will distinguish those seeking Baptism from those  who have already received it.",
      "preparation": "Pray about your desire to follow Christ and your reasons for seeking initiation or full communion  with the Catholic Church. Review the responses and movements of the rite with your OCIA  instructor. Baptized candidates should ensure that the parish has received documentation of their  Baptism. Speak with your sponsor and follow parish instructions concerning arrival time,  seating, attire, rehearsal, and any items you should bring.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Rito conjunto de aceptación y acogida",
      "meaning": "Esta celebración combina el Rito de aceptación en el orden de los catecúmenos para los no bautizados con el Rito de acogida para los candidatos bautizados. Aunque celebran juntos, la Iglesia conserva cuidadosamente sus distintas situaciones sacramentales. Los no bautizados son aceptados como catecúmenos; los cristianos bautizados son acogidos como candidatos que se preparan para completar su iniciación o entrar en la plena comunión católica.",
      "context": "Normalmente marca el paso de la búsqueda inicial a una formación más intencional. Los catecúmenos entran en el catecumenado y comienzan a prepararse para el Bautismo, la Confirmación y la Eucaristía. Los candidatos continúan desarrollando su fe católica mientras se preparan para la plena comunión o para completar su iniciación.",
      "studentActions": "Te reunirás con los demás participantes, tu acompañante y la comunidad, normalmente cerca de la entrada de la iglesia. Podrán preguntarte tu nombre, qué buscas en la Iglesia y si estás dispuesto a seguir a Cristo.\nEl celebrante trazará la señal de la cruz en tu frente, y tu acompañante podrá signar tus otros sentidos. Se te invitará a entrar para escuchar la Palabra de Dios y rezar con la comunidad. Algunas oraciones y preguntas se dirigirán específicamente a los catecúmenos y otras a los candidatos bautizados.",
      "ministerActions": "El celebrante puede preguntar:\n• «¿Qué pides a la Iglesia de Dios?»\n• «¿Qué te ofrece la fe?»\n• «¿Estás dispuesto a comenzar o continuar este camino bajo la guía de Cristo?»\n• «¿Estás dispuesto a escuchar la enseñanza de los apóstoles, reunirte con la comunidad cristiana y unirte a nosotros en la oración y el servicio?»\nTambién puede preguntar a los acompañantes y a la asamblea si están dispuestos a apoyar a catecúmenos y candidatos. La formulación distinguirá a quienes buscan el Bautismo de quienes ya lo recibieron.",
      "preparation": "Reza sobre tu deseo de seguir a Cristo y tus motivos para buscar la iniciación o la plena comunión católica. Repasa las respuestas y movimientos del rito con tu instructor. Los candidatos bautizados deben asegurarse de que la parroquia haya recibido la documentación de su Bautismo. Habla con tu acompañante y sigue las instrucciones sobre llegada, asientos, vestimenta, ensayo y objetos que debas llevar.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "combined-sending",
    "en": {
      "title": "The Combined Rite of Sending Catechumens for Election and Candidates for Recognition",
      "meaning": "In this parish celebration, catechumens and baptized candidates are formally presented before the  community as they prepare to meet the bishop. The parish sends the catechumens to the Rite of  Election and the candidates to the Call to Continuing Conversion. The rite expresses the parish’s  judgment that they are ready to continue toward the Easter sacraments and assures them of the  community’s prayer and support.",
      "context": "This rite normally takes place at the parish near the beginning of Lent, before the diocesan  celebration with the bishop. For catechumens, it prepares for their enrollment among the elect.  For baptized candidates, it prepares for their recognition and Call to Continuing Conversion.  Although celebrated together, the Church maintains the distinction between those awaiting  Baptism and those who are already baptized.",
      "studentActions": "You will be presented to the parish community with your godparent or sponsor. Your godparent  or sponsor will affirm your readiness to continue toward the sacraments. Catechumens may sign  the Book of the Elect, while candidates are recognized separately according to their baptismal  status. The parish community will then pray for you and formally send you to the bishop.",
      "ministerActions": "The celebrant may ask the godparents and sponsors whether the catechumens and candidates  have:\n• Faithfully listened to God’s Word.\n• Responded to that Word through faith and conversion.\n• Participated in the prayer and life of the Christian community.\n• Demonstrated a sincere desire to follow Christ.\n• Shown readiness to continue toward the sacraments. The celebrant may also ask the assembly to affirm its support. Catechumens may be invited to  sign the Book of the Elect, while candidates may be asked to express their intention to continue  growing in faith and conversion.",
      "preparation": "Reflect prayerfully on your journey and the ways Christ has called you to conversion. Speak with  your godparent or sponsor, since that person may be asked to affirm your readiness publicly.  Catechumens should confirm how and when they will sign the Book of the Elect. Follow parish  instructions concerning arrival time, seating, attire, transportation, and the diocesan celebration  with the bishop.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Rito conjunto de envío de catecúmenos para la elección y candidatos para el reconocimiento",
      "meaning": "En esta celebración parroquial, los catecúmenos y candidatos bautizados son presentados formalmente ante la comunidad antes de encontrarse con el obispo. La parroquia envía a los catecúmenos al Rito de elección y a los candidatos al Llamado a la conversión continua. Expresa el juicio de la parroquia de que están preparados para avanzar hacia los sacramentos pascuales y les asegura la oración y el apoyo de la comunidad.",
      "context": "Normalmente tiene lugar en la parroquia cerca del comienzo de la Cuaresma, antes de la celebración diocesana. Prepara a los catecúmenos para su inscripción entre los elegidos y a los candidatos para su reconocimiento y llamado a la conversión continua. Aunque se celebra conjuntamente, mantiene la distinción entre quienes esperan el Bautismo y quienes ya están bautizados.",
      "studentActions": "Serás presentado a la comunidad con tu padrino, madrina o acompañante, que afirmará tu preparación para avanzar hacia los sacramentos. Los catecúmenos podrán firmar el Libro de los Elegidos; los candidatos serán reconocidos por separado según su situación bautismal. La comunidad rezará por ti y te enviará formalmente al obispo.",
      "ministerActions": "El celebrante puede preguntar a padrinos y acompañantes si los catecúmenos y candidatos han:\n• Escuchado fielmente la Palabra de Dios.\n• Respondido mediante la fe y la conversión.\n• Participado en la oración y la vida de la comunidad.\n• Mostrado un deseo sincero de seguir a Cristo.\n• Mostrado preparación para avanzar hacia los sacramentos.\nTambién puede pedir a la asamblea que afirme su apoyo. Los catecúmenos podrán ser invitados a firmar el Libro de los Elegidos; los candidatos, a expresar su intención de seguir creciendo en la fe y la conversión.",
      "preparation": "Reflexiona en oración sobre tu camino y cómo Cristo te ha llamado a la conversión. Habla con tu padrino, madrina o acompañante, que podrá afirmar públicamente tu preparación. Los catecúmenos deben confirmar cómo y cuándo firmarán el Libro de los Elegidos. Sigue las instrucciones sobre llegada, asientos, vestimenta, transporte y la celebración diocesana con el obispo.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "combined-election",
    "en": {
      "title": "The Combined Rite of Election and Call to Continuing Conversion",
      "meaning": "This diocesan celebration brings catechumens and baptized candidates before the bishop while  preserving their distinct sacramental situations. Through the Rite of Election, the Church chooses  the catechumens for the Sacraments of Christian Initiation. They are then called the “elect.”  Through the Call to Continuing Conversion, the Church recognizes the progress of the baptized  candidates and invites them to deepen their conversion as they prepare for full communion or the  completion of their initiation.",
      "context": "This combined rite normally takes place at the cathedral on or near the First Sunday of Lent. It  begins the final period of preparation for the Easter sacraments. The elect enter the Period of  Purification and Enlightenment in preparation for Baptism, Confirmation, and the Eucharist.  Baptized candidates continue their spiritual preparation for Reception into Full Communion,  Confirmation, and the Eucharist—or for the sacraments they have not yet received.",
      "studentActions": "You will be presented to the bishop with your godparent or sponsor and the other participants  from your parish. If you are a catechumen, your godparent will affirm your readiness, your name will be presented  in the Book of the Elect, and you will express your desire to receive the Sacraments of Christian  Initiation. The bishop will then declare you to be one of the elect. If you are a baptized candidate, your sponsor will affirm your spiritual progress. You will express  your intention to continue your conversion and complete your preparation for the sacraments.  The bishop will formally recognize and encourage you.",
      "ministerActions": "The bishop may ask the godparents and sponsors whether the catechumens and candidates have:\n• Faithfully listened to God’s Word.\n• Responded through faith and conversion.\n• Participated in the Church’s prayer and community life.\n• Demonstrated a sincere desire to follow Christ.\n• Shown readiness to continue toward the sacraments. The catechumens may be asked whether they desire to receive Baptism, Confirmation, and the  Eucharist. The candidates may be asked whether they intend to continue their conversion and  deepen their commitment to Christ and his Church. The bishop then formally elects the  catechumens and recognizes the candidates.",
      "preparation": "Prayerfully reflect upon your conversion, your relationship with Christ, and your desire to  receive the sacraments. Speak with your godparent or sponsor, who may be asked to testify  publicly about your readiness. Follow parish instructions regarding registration, transportation,  arrival time, seating, attire, and the Book of the Elect. Because the celebration may include  participants from many parishes, arrive early and remain attentive to the directions given by  parish and diocesan leaders.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Rito conjunto de elección y llamado a la conversión continua",
      "meaning": "Esta celebración diocesana reúne a catecúmenos y candidatos bautizados ante el obispo, conservando sus distintas situaciones sacramentales. Mediante la elección, la Iglesia elige a los catecúmenos para los sacramentos de la iniciación; pasan a llamarse «elegidos». Mediante el llamado a la conversión continua, reconoce el progreso de los candidatos bautizados y los invita a profundizar su conversión hacia la plena comunión o la culminación de su iniciación.",
      "context": "Normalmente se celebra en la catedral el primer domingo de Cuaresma o cerca de esa fecha. Inicia la preparación final para los sacramentos pascuales. Los elegidos entran en el período de purificación e iluminación para el Bautismo, la Confirmación y la Eucaristía. Los candidatos bautizados continúan su preparación espiritual para la plena comunión, la Confirmación y la Eucaristía, o para los sacramentos que aún no han recibido.",
      "studentActions": "Serás presentado al obispo con tu padrino, madrina o acompañante y los demás participantes de tu parroquia.\nSi eres catecúmeno, tu padrino o madrina afirmará tu preparación, tu nombre se presentará en el Libro de los Elegidos y expresarás tu deseo de recibir los sacramentos de la iniciación. El obispo te declarará uno de los elegidos.\nSi eres candidato bautizado, tu acompañante afirmará tu progreso espiritual. Expresarás tu intención de continuar la conversión y completar tu preparación. El obispo te reconocerá y animará formalmente.",
      "ministerActions": "El obispo puede preguntar a padrinos y acompañantes si los catecúmenos y candidatos han:\n• Escuchado fielmente la Palabra de Dios.\n• Respondido con fe y conversión.\n• Participado en la oración y la vida comunitaria de la Iglesia.\n• Mostrado un deseo sincero de seguir a Cristo.\n• Mostrado preparación para avanzar hacia los sacramentos.\nPodrá preguntar a los catecúmenos si desean recibir el Bautismo, la Confirmación y la Eucaristía; y a los candidatos si desean continuar su conversión y profundizar su compromiso con Cristo y su Iglesia. Después elegirá formalmente a los catecúmenos y reconocerá a los candidatos.",
      "preparation": "Reflexiona en oración sobre tu conversión, tu relación con Cristo y tu deseo de recibir los sacramentos. Habla con tu padrino, madrina o acompañante, que podrá dar testimonio público de tu preparación. Sigue las instrucciones sobre inscripción, transporte, llegada, asientos, vestimenta y el Libro de los Elegidos. Como pueden participar muchas parroquias, llega temprano y atiende las indicaciones de los responsables parroquiales y diocesanos.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  },
  {
    "id": "combined-initiation",
    "en": {
      "title": "The Combined Celebration of the Sacraments of Initiation and the Rite of Reception",
      "meaning": "This celebration brings together the initiation of the elect and the reception of baptized  candidates into the full communion of the Catholic Church. The elect receive Baptism,  Confirmation, and the Eucharist. Baptized candidates make a Profession of Faith, are formally  received into full communion, and ordinarily receive Confirmation and the Eucharist. Although celebrated together, their sacramental situations remain distinct. Candidates have  already received Christian Baptism and are never baptized again.",
      "context": "This combined celebration often takes place during the Easter Vigil, the Church’s principal  celebration of Christ’s Resurrection and the traditional time for Christian initiation. It completes  the elect’s preparation for initiation and the candidates’ preparation for full communion. After receiving the sacraments, the newly initiated and newly received enter the Period of  Mystagogy—a time of reflecting upon the mysteries celebrated and growing more deeply in the  Christian life.",
      "studentActions": "If you are one of the elect, you will renounce sin, profess the Church’s faith, and receive  Baptism. You will then receive a white garment and baptismal candle, be confirmed with Sacred  Chrism, and receive the Body and Blood of Christ for the first time. If you are a baptized candidate, you will affirm the faith of the Church and make a formal  Profession of Faith. The celebrant will receive you into the full communion of the Catholic  Church. If you have not already been confirmed, you will ordinarily receive Confirmation before  joining the community in Holy Communion.",
      "ministerActions": "The celebrant will ask the elect to renounce sin and profess their belief in the Father, Son, and  Holy Spirit. He will then baptize them with water in the name of the Father, the Son, and the  Holy Spirit. The celebrant will ask the candidates to profess everything that the Catholic Church believes,  teaches, and proclaims to be revealed by God. He will then formally receive them into full  communion. During Confirmation, the celebrant will anoint each person’s forehead with Sacred Chrism and  say, “Be sealed with the Gift of the Holy Spirit.” Each person responds, “Amen.” All the newly  initiated and received will then be welcomed to the Eucharistic table.",
      "preparation": "Prepare through prayer, reflection, and participation in the final rites and rehearsals. Baptized  candidates should celebrate the Sacrament of Reconciliation before being received into full  communion. Confirm your sacramental records, godparent or sponsor, chosen name if applicable,  clothing, arrival time, seating, and rehearsal instructions with your OCIA team. Your parish will explain what to bring, what to wear, how Baptism will be celebrated, and how to  receive Confirmation and Holy Communion. After the celebration, continue participating in  Mass, parish life, and the Period of Mystagogy.",
      "meaningHeading": "What it is and why it matters",
      "contextHeading": "Its place in OCIA",
      "studentActionsHeading": "What you will do",
      "ministerActionsHeading": "What the celebrant may do or ask",
      "preparationHeading": "How to prepare / parish details"
    },
    "es": {
      "title": "Celebración conjunta de los sacramentos de la iniciación y del rito de recepción",
      "meaning": "Esta celebración reúne la iniciación de los elegidos y la recepción de candidatos bautizados en la plena comunión católica. Los elegidos reciben el Bautismo, la Confirmación y la Eucaristía. Los candidatos hacen una profesión de fe, son recibidos formalmente en la plena comunión y normalmente reciben la Confirmación y la Eucaristía.\nAunque celebran juntos, sus situaciones sacramentales siguen siendo distintas. Los candidatos ya recibieron el Bautismo cristiano y nunca son bautizados otra vez.",
      "context": "A menudo tiene lugar en la Vigilia Pascual, principal celebración de la Resurrección de Cristo y tiempo tradicional de iniciación cristiana. Completa la preparación de los elegidos para la iniciación y de los candidatos para la plena comunión.\nDespués, los recién iniciados y recibidos entran en la mistagogía: un tiempo para reflexionar sobre los misterios celebrados y profundizar en la vida cristiana.",
      "studentActions": "Si eres uno de los elegidos, renunciarás al pecado, profesarás la fe y recibirás el Bautismo. Recibirás una vestidura blanca y una vela bautismal, serás confirmado con el Santo Crisma y recibirás por primera vez el Cuerpo y la Sangre de Cristo.\nSi eres candidato bautizado, afirmarás la fe de la Iglesia y harás una profesión formal de fe. El celebrante te recibirá en la plena comunión católica. Si aún no has sido confirmado, normalmente recibirás la Confirmación antes de unirte a la comunidad en la Comunión.",
      "ministerActions": "El celebrante pedirá a los elegidos renunciar al pecado y profesar su fe en el Padre, el Hijo y el Espíritu Santo. Los bautizará con agua en el nombre del Padre, del Hijo y del Espíritu Santo.\nPedirá a los candidatos profesar todo lo que la Iglesia católica cree, enseña y proclama como revelado por Dios. Después los recibirá formalmente en la plena comunión.\nEn la Confirmación ungirá la frente de cada persona con el Santo Crisma y dirá: «Recibe por esta señal el don del Espíritu Santo». Cada persona responderá: «Amén». Los recién iniciados y recibidos serán acogidos a la mesa eucarística.",
      "preparation": "Prepárate con oración, reflexión y participación en los ritos finales y ensayos. Los candidatos bautizados deben celebrar la Reconciliación antes de ser recibidos en la plena comunión. Confirma con tu equipo los registros sacramentales, padrino, madrina o acompañante, nombre elegido si corresponde, vestimenta, llegada, asientos y ensayo.\nLa parroquia explicará qué llevar, qué vestir, cómo se celebrará el Bautismo y cómo recibir la Confirmación y la Comunión. Después, continúa participando en la Misa, la vida parroquial y la mistagogía.",
      "meaningHeading": "Qué es y por qué es importante",
      "contextHeading": "Su lugar en OCIA",
      "studentActionsHeading": "Qué harás",
      "ministerActionsHeading": "Qué puede hacer o preguntar el celebrante",
      "preparationHeading": "Cómo prepararte / detalles parroquiales"
    }
  }
];
if(typeof module !== 'undefined') module.exports = RiteGuideTemplates;
