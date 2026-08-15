export function averageProgress(items) {
  return items.length
    ? Math.round(items.reduce((sum, item) => sum + Number(item.progressRate), 0) / items.length)
    : 0
}
