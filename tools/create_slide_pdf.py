"""PDF fallback using the reviewed slide renders; editable objects remain in PPTX."""
from pathlib import Path
from reportlab.pdfgen import canvas
ROOT=Path(__file__).resolve().parents[1]
pages=sorted((ROOT/'tmp/slides').glob('slide-*.png'))
if len(pages)!=9:
    raise RuntimeError(f'Expected nine rendered slides, found {len(pages)}')
output=ROOT/'output/documents/GroupB_F0_Presentation.pdf'
c=canvas.Canvas(str(output),pagesize=(960,540))
c.setTitle('Course TA F0 Presentation')
c.setAuthor('Group B')
for image in pages:
    c.drawImage(str(image),0,0,width=960,height=540)
    c.showPage()
c.save()
print(output)
