"""PDF fallback using the reviewed slide renders; editable objects remain in PPTX."""
from pathlib import Path
import argparse
from reportlab.pdfgen import canvas
ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--f1', action='store_true', help='Export the updated 17-slide F0 + F1 presentation')
args=parser.parse_args()
pages=sorted((ROOT/('tmp/slides-f1' if args.f1 else 'tmp/slides')).glob('slide-*.png'))
expected=17 if args.f1 else 9
if len(pages)!=expected:
    raise RuntimeError(f'Expected {expected} rendered slides, found {len(pages)}')
output=ROOT/'output/documents'/('GroupB_F0_F1_Presentation.pdf' if args.f1 else 'GroupB_F0_Presentation.pdf')
c=canvas.Canvas(str(output),pagesize=(960,540))
c.setTitle('Course TA F0 + F1 Presentation' if args.f1 else 'Course TA F0 Presentation')
c.setAuthor('Group B')
for image in pages:
    c.drawImage(str(image),0,0,width=960,height=540)
    c.showPage()
c.save()
print(output)
