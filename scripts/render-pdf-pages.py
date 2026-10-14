from pathlib import Path
import sys

import pymupdf


if len(sys.argv) != 3:
    raise SystemExit("usage: render-pdf-pages.py INPUT_PDF OUTPUT_DIR")

input_path = Path(sys.argv[1]).resolve()
output_dir = Path(sys.argv[2]).resolve()
output_dir.mkdir(parents=True, exist_ok=True)

document = pymupdf.open(input_path)
matrix = pymupdf.Matrix(1.35, 1.35)

for page_index, page in enumerate(document):
    pixmap = page.get_pixmap(matrix=matrix, alpha=False)
    pixmap.save(output_dir / f"page-{page_index + 1:03d}.png")

contact_document = pymupdf.open()
columns = 3
rows = 3
cell_width = 420
cell_height = 594

for group_start in range(0, document.page_count, columns * rows):
    contact_page = contact_document.new_page(
        width=cell_width * columns,
        height=cell_height * rows,
    )
    for offset in range(columns * rows):
        source_index = group_start + offset
        if source_index >= document.page_count:
            break
        column = offset % columns
        row = offset // columns
        rect = pymupdf.Rect(
            column * cell_width + 8,
            row * cell_height + 8,
            (column + 1) * cell_width - 8,
            (row + 1) * cell_height - 8,
        )
        contact_page.show_pdf_page(rect, document, source_index, keep_proportion=True)

for contact_index, contact_page in enumerate(contact_document):
    contact_pixmap = contact_page.get_pixmap(matrix=pymupdf.Matrix(1.0, 1.0), alpha=False)
    contact_pixmap.save(output_dir / f"contact-{contact_index + 1:02d}.png")

print(f"pages={document.page_count}")
