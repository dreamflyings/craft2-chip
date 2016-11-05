package craft

import chisel3._
import chisel3.util._
import cde.{Parameters, Field}
import uncore.tilelink._
import junctions._
import diplomacy._
import fft._
import rocketchip._
import dsptools.numbers._
import dsptools.numbers.implicits._

// FFT with stream interface converted to AXI connected to MMIO manager
class Test(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val stream = new StreamIO(512)
    val axi = new NastiIO().flip
  })

  val fft_config = new FFTConfig(n = 4, p = 4)
  val fft = Module(new DirectFFT(genIn = DspComplex(DspReal(0.0), DspReal(0.0)), config = fft_config))
  
  fft.io <> io.stream
  fft.io.in.sync := io.stream.in.bits.last 
  io.stream.out.bits.last := fft.io.out.sync

  val stream2axi = new NastiIOStreamIOConverter(512)

  stream2axi.io <> io.stream
  io.axi <> stream2axi.io.nasti
}

trait PeripheryTest extends LazyModule {
  val pDevices: ResourceManager[AddrMapEntry]

  pDevices.add(AddrMapEntry("test", MemSize(4096, MemAttr(AddrMapProt.RW))))
}

case object BuildTest extends Field[(ClientUncachedTileLinkIO, Parameters) => Bool]

trait PeripheryTestModule extends HasPeripheryParameters {
  implicit val p: Parameters
  val pBus: TileLinkRecursiveInterconnect
}
