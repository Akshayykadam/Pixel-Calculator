.class Lcom/android/calculator2/abacus/solver/PixelSolverEngine$CachedRates;
.super Ljava/lang/Object;
.source "PixelSolverEngine.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/android/calculator2/abacus/solver/PixelSolverEngine;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0xa
    name = "CachedRates"
.end annotation


# instance fields
.field final rates:Ljava/util/Map;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/Double;",
            ">;"
        }
    .end annotation
.end field

.field final timestamp:J


# direct methods
.method constructor <init>(JLjava/util/Map;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(J",
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/Double;",
            ">;)V"
        }
    .end annotation

    .line 35
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 36
    iput-wide p1, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$CachedRates;->timestamp:J

    .line 37
    iput-object p3, p0, Lcom/android/calculator2/abacus/solver/PixelSolverEngine$CachedRates;->rates:Ljava/util/Map;

    .line 38
    return-void
.end method
