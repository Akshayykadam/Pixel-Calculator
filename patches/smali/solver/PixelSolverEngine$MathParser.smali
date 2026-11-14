.class public Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;
.super Ljava/lang/Object;
.source "PixelSolverEngine.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/android/calculator2/abacus/solver/PixelSolverEngine;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "MathParser"
.end annotation


# instance fields
.field private ch:I

.field private pos:I

.field private final str:Ljava/lang/String;


# direct methods
.method public constructor <init>(Ljava/lang/String;)V
    .locals 1

    .line 1175
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 1173
    const/4 v0, -0x1

    iput v0, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->pos:I

    .line 1176
    iput-object p1, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->str:Ljava/lang/String;

    .line 1177
    return-void
.end method

.method private eat(I)Z
    .locals 2

    .line 1184
    nop

    :goto_0
    iget v0, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    const/16 v1, 0x20

    if-eq v0, v1, :cond_2

    iget v0, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    const/16 v1, 0x9

    if-ne v0, v1, :cond_0

    goto :goto_1

    .line 1185
    :cond_0
    iget v0, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    if-ne v0, p1, :cond_1

    .line 1186
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->nextChar()V

    .line 1187
    const/4 p1, 0x1

    return p1

    .line 1189
    :cond_1
    const/4 p1, 0x0

    return p1

    .line 1184
    :cond_2
    :goto_1
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->nextChar()V

    goto :goto_0
.end method

.method private nextChar()V
    .locals 2

    .line 1180
    iget v0, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->pos:I

    add-int/lit8 v0, v0, 0x1

    iput v0, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->pos:I

    iget-object v1, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->str:Ljava/lang/String;

    invoke-virtual {v1}, Ljava/lang/String;->length()I

    move-result v1

    if-ge v0, v1, :cond_0

    iget-object v0, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->str:Ljava/lang/String;

    iget v1, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->pos:I

    invoke-virtual {v0, v1}, Ljava/lang/String;->charAt(I)C

    move-result v0

    goto :goto_0

    :cond_0
    const/4 v0, -0x1

    :goto_0
    iput v0, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    .line 1181
    return-void
.end method

.method private parseExpression()D
    .locals 4

    .line 1201
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseTerm()D

    move-result-wide v0

    .line 1203
    :goto_0
    const/16 v2, 0x2b

    invoke-direct {p0, v2}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v2

    if-eqz v2, :cond_0

    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseTerm()D

    move-result-wide v2

    add-double/2addr v0, v2

    goto :goto_0

    .line 1204
    :cond_0
    const/16 v2, 0x2d

    invoke-direct {p0, v2}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v2

    if-eqz v2, :cond_1

    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseTerm()D

    move-result-wide v2

    sub-double/2addr v0, v2

    goto :goto_0

    .line 1205
    :cond_1
    return-wide v0
.end method

.method private parseFactor()D
    .locals 8

    .line 1220
    nop

    :goto_0
    iget v0, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    const/16 v1, 0x20

    if-eq v0, v1, :cond_1a

    iget v0, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    const/16 v1, 0x9

    if-ne v0, v1, :cond_0

    goto/16 :goto_6

    .line 1221
    :cond_0
    const/16 v0, 0x2b

    invoke-direct {p0, v0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v0

    if-eqz v0, :cond_1

    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseFactor()D

    move-result-wide v0

    return-wide v0

    .line 1222
    :cond_1
    const/16 v0, 0x2d

    invoke-direct {p0, v0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v0

    if-eqz v0, :cond_2

    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseFactor()D

    move-result-wide v0

    neg-double v0, v0

    return-wide v0

    .line 1225
    :cond_2
    iget v0, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->pos:I

    .line 1226
    const/16 v1, 0x28

    invoke-direct {p0, v1}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v2

    const-string v3, "Missing \')\'"

    const/16 v4, 0x29

    if-eqz v2, :cond_4

    .line 1227
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseExpression()D

    move-result-wide v0

    .line 1228
    invoke-direct {p0, v4}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v2

    if-eqz v2, :cond_3

    goto/16 :goto_5

    :cond_3
    new-instance v0, Ljava/lang/RuntimeException;

    invoke-direct {v0, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 1229
    :cond_4
    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    const/16 v5, 0x39

    const/16 v6, 0x2e

    const/16 v7, 0x30

    if-lt v2, v7, :cond_5

    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    if-le v2, v5, :cond_6

    :cond_5
    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    if-ne v2, v6, :cond_a

    .line 1230
    :cond_6
    :goto_1
    iget v1, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    if-lt v1, v7, :cond_7

    iget v1, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    if-le v1, v5, :cond_8

    :cond_7
    iget v1, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    if-ne v1, v6, :cond_9

    :cond_8
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->nextChar()V

    goto :goto_1

    .line 1231
    :cond_9
    iget-object v1, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->str:Ljava/lang/String;

    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->pos:I

    invoke-virtual {v1, v0, v2}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Ljava/lang/Double;->parseDouble(Ljava/lang/String;)D

    move-result-wide v0

    goto/16 :goto_5

    .line 1232
    :cond_a
    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    const/16 v5, 0x61

    if-lt v2, v5, :cond_19

    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    const/16 v6, 0x7a

    if-gt v2, v6, :cond_19

    .line 1233
    :goto_2
    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    if-lt v2, v5, :cond_b

    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    if-gt v2, v6, :cond_b

    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->nextChar()V

    goto :goto_2

    .line 1234
    :cond_b
    iget-object v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->str:Ljava/lang/String;

    iget v5, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->pos:I

    invoke-virtual {v2, v0, v5}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v0

    .line 1235
    const-string v2, "pi"

    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_c

    .line 1236
    const-wide v0, 0x400921fb54442d18L    # Math.PI

    goto/16 :goto_4

    .line 1237
    :cond_c
    const-string v2, "e"

    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_d

    .line 1238
    const-wide v0, 0x4005bf0a8b145769L    # Math.E

    goto/16 :goto_4

    .line 1240
    :cond_d
    invoke-direct {p0, v1}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v1

    if-eqz v1, :cond_f

    .line 1241
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseExpression()D

    move-result-wide v1

    .line 1242
    invoke-direct {p0, v4}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v4

    if-eqz v4, :cond_e

    goto :goto_3

    :cond_e
    new-instance v0, Ljava/lang/RuntimeException;

    invoke-direct {v0, v3}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 1244
    :cond_f
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseFactor()D

    move-result-wide v1

    .line 1246
    :goto_3
    const-string v3, "sqrt"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_10

    invoke-static {v1, v2}, Ljava/lang/Math;->sqrt(D)D

    move-result-wide v0

    goto :goto_4

    .line 1247
    :cond_10
    const-string v3, "cbrt"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_11

    invoke-static {v1, v2}, Ljava/lang/Math;->cbrt(D)D

    move-result-wide v0

    goto :goto_4

    .line 1248
    :cond_11
    const-string v3, "sin"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_12

    invoke-static {v1, v2}, Ljava/lang/Math;->toRadians(D)D

    move-result-wide v0

    invoke-static {v0, v1}, Ljava/lang/Math;->sin(D)D

    move-result-wide v0

    goto :goto_4

    .line 1249
    :cond_12
    const-string v3, "cos"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_13

    invoke-static {v1, v2}, Ljava/lang/Math;->toRadians(D)D

    move-result-wide v0

    invoke-static {v0, v1}, Ljava/lang/Math;->cos(D)D

    move-result-wide v0

    goto :goto_4

    .line 1250
    :cond_13
    const-string v3, "tan"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_14

    invoke-static {v1, v2}, Ljava/lang/Math;->toRadians(D)D

    move-result-wide v0

    invoke-static {v0, v1}, Ljava/lang/Math;->tan(D)D

    move-result-wide v0

    goto :goto_4

    .line 1251
    :cond_14
    const-string v3, "abs"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_15

    invoke-static {v1, v2}, Ljava/lang/Math;->abs(D)D

    move-result-wide v0

    goto :goto_4

    .line 1252
    :cond_15
    const-string v3, "log"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_16

    invoke-static {v1, v2}, Ljava/lang/Math;->log10(D)D

    move-result-wide v0

    goto :goto_4

    .line 1253
    :cond_16
    const-string v3, "ln"

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_18

    invoke-static {v1, v2}, Ljava/lang/Math;->log(D)D

    move-result-wide v0

    .line 1256
    :goto_4
    nop

    .line 1260
    :goto_5
    const/16 v2, 0x5e

    invoke-direct {p0, v2}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v2

    if-eqz v2, :cond_17

    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseFactor()D

    move-result-wide v2

    invoke-static {v0, v1, v2, v3}, Ljava/lang/Math;->pow(DD)D

    move-result-wide v0

    .line 1262
    :cond_17
    return-wide v0

    .line 1254
    :cond_18
    new-instance v1, Ljava/lang/RuntimeException;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "Unknown function: "

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-direct {v1, v0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw v1

    .line 1257
    :cond_19
    new-instance v0, Ljava/lang/RuntimeException;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "Unexpected: "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    int-to-char v2, v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-direct {v0, v1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 1220
    :cond_1a
    :goto_6
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->nextChar()V

    goto/16 :goto_0
.end method

.method private parseTerm()D
    .locals 4

    .line 1210
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseFactor()D

    move-result-wide v0

    .line 1212
    :goto_0
    const/16 v2, 0x2a

    invoke-direct {p0, v2}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v2

    if-eqz v2, :cond_0

    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseFactor()D

    move-result-wide v2

    mul-double v0, v0, v2

    goto :goto_0

    .line 1213
    :cond_0
    const/16 v2, 0x2f

    invoke-direct {p0, v2}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v2

    if-eqz v2, :cond_1

    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseFactor()D

    move-result-wide v2

    div-double/2addr v0, v2

    goto :goto_0

    .line 1214
    :cond_1
    const/16 v2, 0x25

    invoke-direct {p0, v2}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->eat(I)Z

    move-result v2

    if-eqz v2, :cond_2

    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseFactor()D

    move-result-wide v2

    rem-double/2addr v0, v2

    goto :goto_0

    .line 1215
    :cond_2
    return-wide v0
.end method


# virtual methods
.method public parse()D
    .locals 4

    .line 1193
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->nextChar()V

    .line 1194
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->parseExpression()D

    move-result-wide v0

    .line 1195
    :goto_0
    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    const/16 v3, 0x20

    if-eq v2, v3, :cond_2

    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    const/16 v3, 0x9

    if-ne v2, v3, :cond_0

    goto :goto_1

    .line 1196
    :cond_0
    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->pos:I

    iget-object v3, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->str:Ljava/lang/String;

    invoke-virtual {v3}, Ljava/lang/String;->length()I

    move-result v3

    if-lt v2, v3, :cond_1

    .line 1197
    return-wide v0

    .line 1196
    :cond_1
    new-instance v0, Ljava/lang/RuntimeException;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "Unexpected: "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    iget v2, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->ch:I

    int-to-char v2, v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-direct {v0, v1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 1195
    :cond_2
    :goto_1
    invoke-direct {p0}, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$MathParser;->nextChar()V

    goto :goto_0
.end method
